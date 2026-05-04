package com.example.camundaworker;

import com.example.camundaworker.model.Booking;
import com.example.camundaworker.model.Tool;
import com.example.camundaworker.model.TradeCard;
import com.example.camundaworker.repository.BookingRepository;
import com.example.camundaworker.repository.ToolRepository;
import com.example.camundaworker.repository.TradeCardRepository;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProcessNameWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNameWorker.class);
    private static final Duration MESSAGE_TTL = Duration.ofSeconds(60);

    private final CamundaClient camundaClient;
    private final ToolRepository toolRepository;
    private final BookingRepository bookingRepository;
    private final TradeCardRepository tradeCardRepository;

    public ProcessNameWorker(CamundaClient camundaClient, ToolRepository toolRepository, BookingRepository bookingRepository, TradeCardRepository tradeCardRepository) {
        this.camundaClient = camundaClient;
        this.toolRepository = toolRepository;
        this.bookingRepository = bookingRepository;
        this.tradeCardRepository = tradeCardRepository;
    }

    private Tool getToolOrDefault(String name) {
        return toolRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> new Tool(name, name, 49.99, 15.00, 50.00, 0));
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    private String getVar(Map<String, Object> vars, String key, String fallback) {
        Object val = vars.get(key);
        if (val == null) {
            LOGGER.warn("Variable '{}' not found, using fallback: {}", key, fallback);
            return fallback;
        }
        return val.toString();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr.split("T")[0], DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            LOGGER.error("Failed to parse date: {}", dateStr);
            return LocalDate.now();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getSelectedTools(Map<String, Object> vars) {
        Object tools = vars.get("selectedTools");
        if (tools == null) return List.of();
        if (tools instanceof List) return (List<String>) tools;
        String toolStr = tools.toString().replaceAll("[\\[\\]\"]", "");
        if (toolStr.isBlank()) return List.of();
        return List.of(toolStr.split(",\\s*"));
    }

    private void publishToStartEvent(String messageName, Map<String, Object> variables) {
        LOGGER.info("Publishing '{}' to start event", messageName);
        camundaClient.newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey("")
                .timeToLive(MESSAGE_TTL)
                .variables(variables)
                .send().join();
    }

    private void publishToCatchEvent(String messageName, String correlationKey, Map<String, Object> variables) {
        LOGGER.info("Publishing '{}' with correlationKey={}", messageName, correlationKey);
        camundaClient.newPublishMessageCommand()
                .messageName(messageName)
                .correlationKey(correlationKey)
                .timeToLive(MESSAGE_TTL)
                .variables(variables)
                .send().join();
    }

    // ─────────────────────────────────────────────────────────────────
    // CUSTOMER POOL
    // ─────────────────────────────────────────────────────────────────

    @JobWorker(type = "checkOnlineAvailability")
    public void checkOnlineAvailability(final ActivatedJob job, final JobClient client) {
        LOGGER.info("checkOnlineAvailability triggered");
        List<Map<String, String>> availableTools = toolRepository.findAll().stream()
                .filter(t -> t.getAvailableQuantity() > 0)
                .map(t -> Map.of("label", t.getDisplayName(), "value", t.getName()))
                .toList();
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("onlineAvailable", true, "availableTools", availableTools))
                .send().join();
    }

    @JobWorker(type = "checkInStoreAvailability")
    public void checkInStoreAvailability(final ActivatedJob job, final JobClient client) {
        LOGGER.info("checkInStoreAvailability triggered");
        List<Map<String, String>> availableTools = toolRepository.findAll().stream()
                .filter(t -> t.getAvailableQuantity() > 0)
                .map(t -> Map.of("label", t.getDisplayName(), "value", t.getName()))
                .toList();
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("inStoreAvailable", true, "availableTools", availableTools))
                .send().join();
    }

    @JobWorker(type = "calculatePrice")
    public void calculatePrice(final ActivatedJob job, final JobClient client) {
        LOGGER.info("calculatePrice triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        int quantity = (int) Math.max(1, Double.parseDouble(getVar(vars, "quantity", "1")));
        List<String> selectedTools = getSelectedTools(vars);

        double totalPrice = 0.0;
        StringBuilder priceBreakdown = new StringBuilder();

        if (selectedTools.isEmpty()) {
            totalPrice = quantity * 49.99;
            priceBreakdown.append("Default item x").append(quantity).append(" = £").append(totalPrice);
        } else {
            for (String tool : selectedTools) {
                Tool t = getToolOrDefault(tool.trim().toLowerCase());
                double lineTotal = t.getPurchasePrice() * quantity;
                totalPrice += lineTotal;
                priceBreakdown.append(t.getDisplayName()).append(" x").append(quantity)
                        .append(" @ £").append(t.getPurchasePrice()).append(" = £").append(round2(lineTotal)).append(" | ");
            }
        }

        totalPrice = round2(totalPrice);

        // Validate trade card number against DB if customer claims membership
        boolean isTradeCardMember = Boolean.parseBoolean(getVar(vars, "isTradeCardMember", "false"));
        String submittedCardNumber = getVar(vars, "tradeCardNumber", "");
        boolean tradeCardValid = false;
        if (isTradeCardMember && !submittedCardNumber.isEmpty()) {
            tradeCardValid = tradeCardRepository.findByCardNumberIgnoreCase(submittedCardNumber)
                    .map(TradeCard::isActive)
                    .orElse(false);
            if (!tradeCardValid) {
                LOGGER.warn("Trade card '{}' not found in DB — membership rejected", submittedCardNumber);
                isTradeCardMember = false;
            } else {
                LOGGER.info("Trade card '{}' validated successfully", submittedCardNumber);
            }
        }

        // Apply 10% Trade Card loyalty discount
        double discountAmount = 0.0;
        if (tradeCardValid) {
            discountAmount = round2(totalPrice * 0.10);
            totalPrice = round2(totalPrice - discountAmount);
            priceBreakdown.append("Trade Card Discount (10%): -£").append(discountAmount);
            LOGGER.info("Trade card discount applied: -£{} | Discounted total: £{}", discountAmount, totalPrice);
        }

        LOGGER.info("Price breakdown: {} | Total: £{} | tradeCardValid={}", priceBreakdown, totalPrice, tradeCardValid);

        client.newCompleteCommand(job.getKey())
                .variables(Map.of(
                        "totalAmount", totalPrice,
                        "priceBreakdown", priceBreakdown.toString(),
                        "isTradeCardMember", isTradeCardMember,
                        "tradeCardValid", tradeCardValid,
                        "discountAmount", discountAmount
                ))
                .send().join();
    }

    @JobWorker(type = "registerMember")
    public void registerMember(final ActivatedJob job, final JobClient client) {
        LOGGER.info("registerMember triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String firstName = getVar(vars, "customerFirstName", "Customer");
        String lastName = getVar(vars, "customerLastName", "");
        String holderName = (firstName + " " + lastName).trim();
        long cardDigits = 1_000_000_000L + (System.currentTimeMillis() % 9_000_000_000L);
        String tradeCardNumber = "TC-" + cardDigits;
        tradeCardRepository.save(new TradeCard(tradeCardNumber, holderName, true));
        LOGGER.info("Trade card issued and saved to DB | holder={} | card={}", holderName, tradeCardNumber);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of(
                        "membershipRegistered", true,
                        "isTradeCardMember", true,
                        "tradeCardNumber", tradeCardNumber
                ))
                .send().join();
    }

    @JobWorker(type = "processPayment")
    public void processPayment(final ActivatedJob job, final JobClient client) {
        LOGGER.info("processPayment triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        double totalAmount = Double.parseDouble(getVar(vars, "totalAmount", "0"));
        String paymentMethod = getVar(vars, "paymentMethod", "card");
        LOGGER.info("Processing payment of £{} via {}", totalAmount, paymentMethod);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("paymentSuccessful", true))
                .send().join();
    }

    @JobWorker(type = "sendCreditApplication")
    public void sendCreditApplication(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendCreditApplication triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = String.valueOf(job.getProcessInstanceKey());
        vars.put("customerProcessKey", customerProcessKey);
        publishToStartEvent("creditApplication", vars);
        LOGGER.info("Credit application sent | customerProcessKey={}", customerProcessKey);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("customerProcessKey", customerProcessKey))
                .send().join();
    }

    @JobWorker(type = "sendPurchaseDetail")
    public void sendPurchaseDetail(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendPurchaseDetail triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        // preserve the original key if passed through from the hire confirmation flow
        String customerProcessKey = vars.containsKey("customerProcessKey")
                ? getVar(vars, "customerProcessKey", String.valueOf(job.getProcessInstanceKey()))
                : String.valueOf(job.getProcessInstanceKey());
        vars.put("customerProcessKey", customerProcessKey);
        publishToStartEvent("purchaseDetail", vars);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("customerProcessKey", customerProcessKey))
                .send().join();
    }

    @JobWorker(type = "notifyPickupReady")
    public void notifyPickupReady(final ActivatedJob job, final JobClient client) {
        LOGGER.info("notifyPickupReady triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = vars.containsKey("customerProcessKey")
                ? getVar(vars, "customerProcessKey", String.valueOf(job.getProcessInstanceKey()))
                : String.valueOf(job.getProcessInstanceKey());
        Map<String, Object> payload = new HashMap<>(vars);
        payload.put("customerProcessKey", customerProcessKey);
        publishToCatchEvent("pickupReady", customerProcessKey, payload);
        LOGGER.info("Pickup ready notification sent | customerProcessKey={}", customerProcessKey);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("customerProcessKey", customerProcessKey))
                .send().join();
    }

    @JobWorker(type = "sendDeliveryRequirements")
    public void sendDeliveryRequirements(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendDeliveryRequirements triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        // preserve the original key if passed through from the hire confirmation flow
        String customerProcessKey = vars.containsKey("customerProcessKey")
                ? getVar(vars, "customerProcessKey", String.valueOf(job.getProcessInstanceKey()))
                : String.valueOf(job.getProcessInstanceKey());
        vars.put("customerProcessKey", customerProcessKey);
        publishToStartEvent("toolRequest", vars);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("customerProcessKey", customerProcessKey))
                .send().join();
    }

    @JobWorker(type = "sendReturn")
    public void sendReturn(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendReturn triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = getVar(vars, "customerProcessKey", "NOT_FOUND");
        publishToCatchEvent("toolReturn", customerProcessKey, Map.of(
                "toolReturned", true,
                "customerProcessKey", customerProcessKey
        ));
        LOGGER.info("Tool return sent | customerProcessKey={}", customerProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    @JobWorker(type = "sendHireRequest")
    public void sendHireRequest(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendHireRequest triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = String.valueOf(job.getProcessInstanceKey());
        vars.put("customerProcessKey", customerProcessKey);
        publishToStartEvent("hireRequest", vars);
        LOGGER.info("Hire request sent | customerProcessKey={}", customerProcessKey);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("customerProcessKey", customerProcessKey))
                .send().join();
    }

    // ─────────────────────────────────────────────────────────────────
    // FINTRUST UK POOL
    // ─────────────────────────────────────────────────────────────────

    @JobWorker(type = "processCreditApplication")
    public void processCreditApplication(final ActivatedJob job, final JobClient client) {
        LOGGER.info("processCreditApplication triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        double amount = Double.parseDouble(getVar(vars, "totalAmount", "500"));
        boolean approved = amount >= 100.0 && amount < 5000.0;
        String declineReason = amount < 100.0
                ? "Finance is only available for purchases of £100 or more"
                : amount >= 5000.0
                ? "Amount exceeds the maximum finance limit of £5,000"
                : "";
        LOGGER.info("Credit application | amount=£{} | approved={} | reason={}", amount, approved, declineReason.isEmpty() ? "OK" : declineReason);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("creditApproved", approved, "declineReason", declineReason))
                .send().join();
    }

    @JobWorker(type = "logApproved")
    public void logApproved(final ActivatedJob job, final JobClient client) {
        LOGGER.info("logApproved: credit application approved");
        Map<String, Object> vars = job.getVariablesAsMap();
        double totalAmount = Double.parseDouble(getVar(vars, "totalAmount", "0"));
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("creditApproved", true, "applicationStatus", "approved", "financeAmount", totalAmount))
                .send().join();
    }

    @JobWorker(type = "logDeclined")
    public void logDeclined(final ActivatedJob job, final JobClient client) {
        LOGGER.info("logDeclined: credit application declined");
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("creditApproved", false, "applicationStatus", "declined"))
                .send().join();
    }

    @JobWorker(type = "sendRepaymentPlan")
    public void sendRepaymentPlan(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendRepaymentPlan triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = getVar(vars, "customerProcessKey", "NOT_FOUND");
        double monthlyPayment = Double.parseDouble(getVar(vars, "monthlyPayment", "0"));
        String repaymentPeriod = getVar(vars, "repaymentPeriod", "12");
        LOGGER.info("Repayment plan sent | monthly=£{} | period={}mo | customerKey={}", monthlyPayment, repaymentPeriod, customerProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    @JobWorker(type = "sendFinanceDecision")
    public void sendFinanceDecision(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendFinanceDecision triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        boolean approved = Boolean.parseBoolean(getVar(vars, "creditApproved", "false"));
        String customerProcessKey = getVar(vars, "customerProcessKey", "NOT_FOUND");

        String purchaseOrHire = getVar(vars, "purchaseOrHire", "purchase");

        Map<String, Object> payload = new HashMap<>();
        payload.put("creditApproved", approved);
        payload.put("paymentSuccessful", approved); // set paymentSuccessful based on credit decision
        payload.put("purchaseOrHire", purchaseOrHire);

        publishToCatchEvent("financeDecision", customerProcessKey, payload);
        LOGGER.info("Finance decision sent | approved={} | customerProcessKey={}", approved, customerProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    @JobWorker(type = "setupRepaymentSchedule")
    public void setupRepaymentSchedule(final ActivatedJob job, final JobClient client) {
        LOGGER.info("setupRepaymentSchedule triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String repaymentPeriod = getVar(vars, "repaymentPeriod", "12");
        double totalAmount = Double.parseDouble(getVar(vars, "totalAmount", "0"));
        double monthlyPayment = round2(totalAmount / Integer.parseInt(repaymentPeriod));
        LOGGER.info("Repayment schedule set | period={}mo | monthly=£{}", repaymentPeriod, monthlyPayment);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of(
                        "repaymentPeriod", repaymentPeriod,
                        "monthlyPayment", monthlyPayment,
                        "financeAmount", totalAmount
                ))
                .send().join();
    }

    // ─────────────────────────────────────────────────────────────────
    // PROBUILD LTD POOL — TOOL HIRE TEAM
    // ─────────────────────────────────────────────────────────────────

    @JobWorker(type = "checkToolAvailability")
    public void checkToolAvailability(final ActivatedJob job, final JobClient client) {
        LOGGER.info("checkToolAvailability triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        List<String> selectedTools = getSelectedTools(vars);
        int quantity = (int) Math.max(1, Double.parseDouble(getVar(vars, "quantity", "1")));
        boolean available = selectedTools.isEmpty() || selectedTools.stream()
                .allMatch(t -> toolRepository.findByNameIgnoreCase(t.trim())
                        .map(tool -> tool.getAvailableQuantity() >= quantity)
                        .orElse(false));
        LOGGER.info("Availability check for {} | qty={} | available={}", selectedTools, quantity, available);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("toolAvailable", available))
                .send().join();
    }

    @JobWorker(type = "confirmHireBooking")
    public void confirmHireBooking(final ActivatedJob job, final JobClient client) {
        LOGGER.info("confirmHireBooking triggered");
        Map<String, Object> vars = job.getVariablesAsMap();

        // Parse hire dates and calculate duration
        String startDateStr = getVar(vars, "hireStartDate", "");
        String endDateStr = getVar(vars, "hireEndDate", "");
        int hireDuration = 1;
        if (!startDateStr.isEmpty() && !endDateStr.isEmpty()) {
            LocalDate startDate = parseDate(startDateStr);
            LocalDate endDate = parseDate(endDateStr);
            hireDuration = (int) ChronoUnit.DAYS.between(startDate, endDate);
            if (hireDuration < 1) hireDuration = 1;
        }
        LOGGER.info("Hire duration: {} days", hireDuration);

        // Calculate hire cost and deposit per tool from DB
        List<String> selectedTools = getSelectedTools(vars);
        int quantity = (int) Math.max(1, Double.parseDouble(getVar(vars, "quantity", "1")));
        double totalHireCost = 0.0;
        double totalDeposit = 0.0;
        StringBuilder hireBreakdown = new StringBuilder();

        if (selectedTools.isEmpty()) {
            totalHireCost = hireDuration * 15.00;
            totalDeposit = 50.00;
            hireBreakdown.append("Default tool x").append(hireDuration).append(" days");
        } else {
            for (String tool : selectedTools) {
                Tool t = getToolOrDefault(tool.trim().toLowerCase());
                double lineCost = t.getHireRatePerDay() * hireDuration;
                totalHireCost += lineCost;
                totalDeposit += t.getDepositAmount();
                hireBreakdown.append(t.getDisplayName())
                        .append(" @ £").append(t.getHireRatePerDay()).append("/day x ")
                        .append(hireDuration).append(" days = £").append(round2(lineCost))
                        .append(" | Deposit: £").append(t.getDepositAmount()).append(" | ");
            }
        }

        totalHireCost = round2(totalHireCost);
        totalDeposit = round2(totalDeposit);
        double totalAmount = round2(totalHireCost + totalDeposit);
        String bookingReference = "BK-" + System.currentTimeMillis();

        // Persist booking to database
        LocalDate startDate = startDateStr.isEmpty() ? LocalDate.now() : parseDate(startDateStr);
        LocalDate endDate = endDateStr.isEmpty() ? LocalDate.now().plusDays(hireDuration) : parseDate(endDateStr);
        String firstName = getVar(vars, "customerFirstName", "");
        String lastName = getVar(vars, "customerLastName", "");
        String email = getVar(vars, "customerEmail", "");
        String phone = getVar(vars, "customerPhone", "");
        Booking booking = new Booking(bookingReference, firstName, lastName, email, phone,
                String.join(", ", selectedTools), quantity, startDate, endDate,
                hireDuration, totalHireCost, totalDeposit, totalAmount);
        bookingRepository.save(booking);

        // Decrement stock for each hired tool
        for (String toolName : selectedTools) {
            toolRepository.findByNameIgnoreCase(toolName.trim()).ifPresent(tool -> {
                int newQty = Math.max(0, tool.getAvailableQuantity() - quantity);
                tool.setAvailableQuantity(newQty);
                toolRepository.save(tool);
                LOGGER.info("Tool '{}' reserved | stock reduced by {} | remaining={}", tool.getDisplayName(), quantity, newQty);
            });
        }

        LOGGER.info("Booking saved to DB | ref={} | total=£{} | customer={} {}", bookingReference, totalAmount, firstName, lastName);

        client.newCompleteCommand(job.getKey())
                .variables(Map.of(
                        "bookingReference", bookingReference,
                        "orderId", bookingReference,
                        "hireDuration", hireDuration,
                        "totalHireCost", totalHireCost,
                        "depositAmount", totalDeposit,
                        "totalAmount", totalAmount,
                        "hireBreakdown", hireBreakdown.toString()
                ))
                .send().join();
    }

    // Sends ALL variables back to Customer so the hire summary form can display them
    @JobWorker(type = "sendBookingConfirmed")
    public void sendBookingConfirmed(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendBookingConfirmed triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = getVar(vars, "customerProcessKey", "NOT_FOUND");
        String bookingReference = getVar(vars, "bookingReference", "BK-UNKNOWN");

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingReference", bookingReference);
        payload.put("hireDuration", vars.getOrDefault("hireDuration", 1));
        payload.put("totalHireCost", vars.getOrDefault("totalHireCost", 0));
        payload.put("depositAmount", vars.getOrDefault("depositAmount", 0));
        payload.put("totalAmount", vars.getOrDefault("totalAmount", 0));
        payload.put("hireBreakdown", vars.getOrDefault("hireBreakdown", ""));
        payload.put("selectedTools", vars.getOrDefault("selectedTools", ""));
        payload.put("quantity", vars.getOrDefault("quantity", 0));
        payload.put("hireStartDate", vars.getOrDefault("hireStartDate", ""));
        payload.put("hireEndDate", vars.getOrDefault("hireEndDate", ""));
        payload.put("toolAvailable", true);
        payload.put("purchaseOrHire", vars.getOrDefault("purchaseOrHire", "hire"));
        payload.put("customerProcessKey", customerProcessKey);
        payload.put("customerFirstName", vars.getOrDefault("customerFirstName", ""));
        payload.put("customerLastName", vars.getOrDefault("customerLastName", ""));
        payload.put("customerEmail", vars.getOrDefault("customerEmail", ""));
        payload.put("customerPhone", vars.getOrDefault("customerPhone", ""));

        publishToCatchEvent("hireConfirmed", customerProcessKey, payload);
        LOGGER.info("Booking confirmed sent | ref={} | customerProcessKey={}", bookingReference, customerProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    @JobWorker(type = "sendToolUnavailable")
    public void sendToolUnavailable(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendToolUnavailable triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = getVar(vars, "customerProcessKey", "NOT_FOUND");
        publishToCatchEvent("toolUnavailable", customerProcessKey, Map.of("toolAvailable", false));
        LOGGER.info("Tool unavailable sent | customerProcessKey={}", customerProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    // ─────────────────────────────────────────────────────────────────
    // PROBUILD LTD POOL — LOGISTICS TEAM
    // ─────────────────────────────────────────────────────────────────

    @JobWorker(type = "notifyCustomer")
    public void notifyCustomer(final ActivatedJob job, final JobClient client) {
        LOGGER.info("notifyCustomer triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String customerProcessKey = getVar(vars, "customerProcessKey", "NOT_FOUND");
        String deliveryStatus = getVar(vars, "deliveryStatus", "Delivered");
        String actualDeliveryDate = getVar(vars, "actualDeliveryDate", "");
        String purchaseOrHire = getVar(vars, "purchaseOrHire", "purchase");
        publishToCatchEvent("packageDelivered", customerProcessKey, Map.of(
                "deliveryStatus", deliveryStatus,
                "actualDeliveryDate", actualDeliveryDate,
                "purchaseOrHire", purchaseOrHire
        ));
        LOGGER.info("Customer notified | status={} | customerProcessKey={}", deliveryStatus, customerProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    // ─────────────────────────────────────────────────────────────────
    // PROBUILD LTD POOL — WAREHOUSE TEAM
    // ─────────────────────────────────────────────────────────────────

    @JobWorker(type = "markToolAvailable")
    public void markToolAvailable(final ActivatedJob job, final JobClient client) {
        LOGGER.info("markToolAvailable triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        List<String> selectedTools = getSelectedTools(vars);
        int quantity = (int) Math.max(1, Double.parseDouble(getVar(vars, "quantity", "1")));
        for (String toolName : selectedTools) {
            toolRepository.findByNameIgnoreCase(toolName.trim()).ifPresent(tool -> {
                tool.setAvailableQuantity(tool.getAvailableQuantity() + quantity);
                toolRepository.save(tool);
                LOGGER.info("Tool '{}' returned to stock | new qty={}", tool.getDisplayName(), tool.getAvailableQuantity());
            });
        }
        bookingRepository.findByBookingReference(getVar(vars, "bookingReference", ""))
                .ifPresent(b -> { b.setStatus("RETURNED"); bookingRepository.save(b); });
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("toolStatus", "available"))
                .send().join();
    }

    @JobWorker(type = "sendApproval")
    public void sendApproval(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendApproval triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        // probuildProcessKey from vars is the ProBuild warehouse instance key — must not be overwritten
        publishToStartEvent("repairApproved", vars);
        LOGGER.info("Repair approval sent | probuildProcessKey={}", vars.get("probuildProcessKey"));
        client.newCompleteCommand(job.getKey()).send().join();
    }

    // ─────────────────────────────────────────────────────────────────
    // FIXPRO LTD POOL
    // ─────────────────────────────────────────────────────────────────

    @JobWorker(type = "toolsForMaintenance")
    public void sendToolsForMaintenance(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendToolsForMaintenance triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String probuildProcessKey = String.valueOf(job.getProcessInstanceKey());
        vars.put("probuildProcessKey", probuildProcessKey);
        publishToStartEvent("toolsForMaintenance", vars);
        LOGGER.info("Tools sent for maintenance | probuildProcessKey={}", probuildProcessKey);
        client.newCompleteCommand(job.getKey())
                .variables(Map.of("probuildProcessKey", probuildProcessKey))
                .send().join();
    }

    @JobWorker(type = "sendRepairAuthorisation")
    public void sendRepairAuthorisation(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendRepairAuthorisation triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        double repairCost = Double.parseDouble(getVar(vars, "repairCost", "0"));
        boolean requiresManagerAuth = repairCost > 50.0;
        if (requiresManagerAuth) {
            LOGGER.info("Repair cost £{} exceeds £50 threshold — ProBuild Tool Hire Manager authorisation required", repairCost);
        } else {
            LOGGER.info("Repair cost £{} within auto-approval limit (≤£50)", repairCost);
        }
        Map<String, Object> payload = new HashMap<>(vars);
        payload.put("repairApproved", true);
        payload.put("requiresManagerAuth", requiresManagerAuth);
        payload.put("repairCost", repairCost);
        publishToStartEvent("repairAuthorisation", payload);
        LOGGER.info("Repair authorisation sent | cost=£{} | requiresManagerAuth={}", repairCost, requiresManagerAuth);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    @JobWorker(type = "sendDisapproval")
    public void sendDisapproval(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendDisapproval triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        // pass all vars so FixPro has probuildProcessKey to correlate toolsReturned
        publishToStartEvent("repairDisapproved", vars);
        client.newCompleteCommand(job.getKey()).send().join();
    }

    @JobWorker(type = "sendToolsBack")
    public void sendToolsBack(final ActivatedJob job, final JobClient client) {
        LOGGER.info("sendToolsBack triggered");
        Map<String, Object> vars = job.getVariablesAsMap();
        String probuildProcessKey = getVar(vars, "probuildProcessKey", "NOT_FOUND");
        Map<String, Object> payload = new HashMap<>(vars);
        publishToCatchEvent("toolsReturned", probuildProcessKey, payload);
        LOGGER.info("Tools returned to ProBuild | probuildProcessKey={}", probuildProcessKey);
        client.newCompleteCommand(job.getKey()).send().join();
    }
}