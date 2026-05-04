package com.example.camundaworker;

import com.example.camundaworker.model.Booking;
import com.example.camundaworker.model.Tool;
import com.example.camundaworker.repository.BookingRepository;
import com.example.camundaworker.repository.ToolRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final BookingRepository bookingRepository;
    private final ToolRepository toolRepository;

    public ApiController(BookingRepository bookingRepository, ToolRepository toolRepository) {
        this.bookingRepository = bookingRepository;
        this.toolRepository = toolRepository;
    }

    // ── Tools ────────────────────────────────────────────────────────────

    @GetMapping("/tools")
    public List<Tool> getAllTools() {
        return toolRepository.findAll();
    }

    @GetMapping("/tools/available")
    public List<Tool> getAvailableTools() {
        return toolRepository.findAll().stream()
                .filter(t -> t.getAvailableQuantity() > 0)
                .toList();
    }

    // ── Bookings ─────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @GetMapping("/bookings/{reference}")
    public ResponseEntity<Booking> getBooking(@PathVariable String reference) {
        return bookingRepository.findByBookingReference(reference)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bookings/status/{status}")
    public List<Booking> getBookingsByStatus(@PathVariable String status) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    // ── Summary ───────────────────────────────────────────────────────────

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        List<Booking> bookings = bookingRepository.findAll();
        long confirmed = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        long returned = bookings.stream().filter(b -> "RETURNED".equals(b.getStatus())).count();
        double revenue = bookings.stream().mapToDouble(Booking::getTotalAmount).sum();
        return Map.of(
                "totalBookings", bookings.size(),
                "confirmedBookings", confirmed,
                "returnedBookings", returned,
                "totalRevenue", Math.round(revenue * 100.0) / 100.0,
                "totalTools", toolRepository.count(),
                "availableToolTypes", toolRepository.findAll().stream().filter(t -> t.getAvailableQuantity() > 0).count()
        );
    }
}
