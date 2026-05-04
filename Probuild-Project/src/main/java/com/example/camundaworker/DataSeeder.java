package com.example.camundaworker;

import com.example.camundaworker.model.Tool;
import com.example.camundaworker.model.TradeCard;
import com.example.camundaworker.repository.ToolRepository;
import com.example.camundaworker.repository.TradeCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    private final ToolRepository toolRepository;
    private final TradeCardRepository tradeCardRepository;

    public DataSeeder(ToolRepository toolRepository, TradeCardRepository tradeCardRepository) {
        this.toolRepository = toolRepository;
        this.tradeCardRepository = tradeCardRepository;
    }

    @Override
    public void run(String... args) {
        if (toolRepository.count() > 0) return;

        // ── Power Tools ──────────────────────────────────────────────────
        toolRepository.save(new Tool("cordless-drill",    "Cordless Drill",           80.00, 12.00,  30.00, 15));
        toolRepository.save(new Tool("hammer-drill",      "Hammer Drill (SDS)",      110.00, 18.00,  40.00,  8));
        toolRepository.save(new Tool("angle-grinder",     "Angle Grinder",            55.00, 10.00,  25.00, 12));
        toolRepository.save(new Tool("circular-saw",      "Circular Saw",             75.00, 12.00,  30.00,  8));
        toolRepository.save(new Tool("jigsaw",            "Jigsaw",                   60.00, 10.00,  25.00, 10));
        toolRepository.save(new Tool("reciprocating-saw", "Reciprocating Saw",        70.00, 12.00,  28.00,  7));
        toolRepository.save(new Tool("floor-sander",      "Floor Sander",            180.00, 35.00,  75.00,  3));
        toolRepository.save(new Tool("electric-planer",   "Electric Planer",          70.00, 12.00,  25.00,  7));
        toolRepository.save(new Tool("router",            "Wood Router",              85.00, 14.00,  30.00,  6));
        toolRepository.save(new Tool("paint-sprayer",     "Paint Sprayer",            90.00, 15.00,  35.00,  6));

        // ── Garden & Outdoor ─────────────────────────────────────────────
        toolRepository.save(new Tool("strimmer",          "Strimmer",                 65.00, 10.00,  25.00, 10));
        toolRepository.save(new Tool("leaf-blower",       "Leaf Blower",              50.00,  8.00,  20.00, 10));
        toolRepository.save(new Tool("chainsaw",          "Chainsaw",                140.00, 28.00,  60.00,  5));
        toolRepository.save(new Tool("lawnmower",         "Petrol Lawnmower",        150.00, 25.00,  55.00,  6));
        toolRepository.save(new Tool("rotavator",         "Rotavator",               220.00, 45.00,  90.00,  3));

        // ── Heavy Equipment ──────────────────────────────────────────────
        toolRepository.save(new Tool("cement-mixer",      "Cement Mixer",            120.00, 25.00,  50.00,  5));
        toolRepository.save(new Tool("pressure-washer",   "Pressure Washer",          95.00, 18.00,  40.00,  8));
        toolRepository.save(new Tool("tile-cutter",       "Tile Cutter",              85.00, 15.00,  35.00,  6));
        toolRepository.save(new Tool("scaffolding",       "Scaffolding",             200.00, 40.00, 100.00,  3));
        toolRepository.save(new Tool("concrete-breaker",  "Concrete Breaker",        350.00, 60.00, 120.00,  3));
        toolRepository.save(new Tool("generator",         "Generator",               250.00, 45.00, 100.00,  4));
        toolRepository.save(new Tool("dehumidifier",      "Dehumidifier",            130.00, 20.00,  50.00,  5));
        toolRepository.save(new Tool("air-compressor",    "Air Compressor",          160.00, 30.00,  65.00,  4));

        // ── Access & Safety ──────────────────────────────────────────────
        toolRepository.save(new Tool("ladder",            "Extension Ladder",         70.00,  8.00,  20.00, 12));
        toolRepository.save(new Tool("podium-steps",      "Podium Steps",             90.00, 12.00,  30.00,  8));
        toolRepository.save(new Tool("wheelbarrow",       "Wheelbarrow",              40.00,  5.00,  15.00, 20));
        toolRepository.save(new Tool("site-lighting",     "Site Lighting (set)",      60.00, 10.00,  25.00,  8));

        LOGGER.info("Tool catalogue seeded with {} tools", toolRepository.count());

        // ── Trade Card Members ────────────────────────────────────────────
        tradeCardRepository.save(new TradeCard("TC-1000000001", "James Builder",   true));
        tradeCardRepository.save(new TradeCard("TC-1000000002", "Sarah Contractor", true));
        tradeCardRepository.save(new TradeCard("TC-1000000003", "Mike Tradesman",  true));
        tradeCardRepository.save(new TradeCard("TC-1000000004", "Lisa Plumber",    true));
        tradeCardRepository.save(new TradeCard("TC-1000000005", "Dave Electrician", true));
        LOGGER.info("Trade card members seeded with {} records", tradeCardRepository.count());
    }
}
