package com.busbooking;

import com.busbooking.service.BusBookingService;
import com.busbooking.model.Bus;
import com.busbooking.model.Booking;
import com.busbooking.model.Passenger;
import com.busbooking.io.TicketFileHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Console entry point for the Bus Booking System.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   ONLINE BUS TICKET BOOKING SYSTEM (JAVA & SQL)  ");
        System.out.println("==================================================");

        try {
            // 1. Initialize Service
            BusBookingService service = new BusBookingService();

            // 2. Search buses using Java Streams
            System.out.println("\n>>> STEP 1: Searching buses from Koyambedu to Coimbatore...");
            List<Bus> results = service.searchBuses("Koyambedu (Chennai)", "Coimbatore", "2026-10-24");
            System.out.println("Found " + results.size() + " bus operators.");

            // 3. Sort by price using Stream API & Lambda
            System.out.println("\n>>> STEP 2: Sorting buses by Lowest Fare (Stream API):");
            List<Bus> sortedByPrice = service.getBusesSortedByPrice(results);
            sortedByPrice.forEach(b -> System.out.println("  * " + b.getOperatorName() + " - ₹" + b.getBaseFare() + " (Rating: " + b.getRating() + ")"));

            // 4. Select top rated bus
            Bus selectedBus = results.get(0);
            System.out.println("\n>>> STEP 3: Selected Bus: " + selectedBus.getOperatorName());

            // 5. Create Passenger
            Passenger passenger = new Passenger("Ratheesh Kumar", 22, "Male", "9876543210", "ratheesh@example.com");

            // 6. Book seats (Lower L1 & Upper U2)
            List<String> chosenSeats = Arrays.asList("L1", "U2");
            System.out.println("\n>>> STEP 4: Booking seats: " + chosenSeats + " for " + passenger.getFullName());
            Booking booking = service.bookTicket(selectedBus.getBusId(), passenger, chosenSeats, "2026-10-24");

            // 7. Generate Ticket File using Character Streams
            System.out.println("\n>>> STEP 5: Generating e-Ticket via Character Stream (I/O)...");
            String ticketFile = TicketFileHandler.saveTicketToFile(booking, selectedBus.getOperatorName(), "Chennai -> Coimbatore");
            System.out.println("Ticket file created: " + ticketFile);

            System.out.println("\n>>> STEP 6: Reading back generated ticket:");
            String readContent = TicketFileHandler.readTicketFromFile(ticketFile);
            System.out.println(readContent);

            System.out.println("All operations completed successfully!");

        } catch (Exception e) {
            System.err.println("Execution Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
