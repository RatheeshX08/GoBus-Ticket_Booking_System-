package com.busbooking.io;

import com.busbooking.model.Booking;
import com.busbooking.model.Seat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Character Stream file handler for generating and reading e-ticket receipts.
 */
public class TicketFileHandler {

    public static String saveTicketToFile(Booking booking, String operatorName, String route) throws IOException {
        String fileName = "ticket_" + booking.getPnr() + ".txt";
        File file = new File(fileName);

        try (FileWriter fw = new FileWriter(file);
             PrintWriter pw = new PrintWriter(fw)) {

            pw.println("=================================================");
            pw.println("             BUS TICKET E-RECEIPT                ");
            pw.println("=================================================");
            pw.println("PNR Number       : " + booking.getPnr());
            pw.println("Bus Operator     : " + operatorName);
            pw.println("Route            : " + route);
            pw.println("Journey Date     : " + booking.getJourneyDate());
            pw.println("-------------------------------------------------");
            pw.println("Passenger Name   : " + booking.getPassenger().getFullName());
            pw.println("Age / Gender     : " + booking.getPassenger().getAge() + " yrs / " + booking.getPassenger().getGender());
            pw.println("Contact Phone    : " + booking.getPassenger().getPhone());
            pw.println("-------------------------------------------------");
            pw.print("Booked Seats     : ");
            for (Seat seat : booking.getBookedSeats()) {
                pw.print(seat.getSeatNumber() + " ");
            }
            pw.println();
            pw.println("Total Paid Fare  : ₹" + String.format("%.2f", booking.getTotalAmount()));
            pw.println("Status           : " + booking.getBookingStatus());
            pw.println("=================================================");
            pw.println("    Thank you for traveling with us! Safe Trip!   ");
            pw.println("=================================================");
        }

        System.out.println("[I/O] Ticket receipt saved to: " + file.getAbsolutePath());
        return fileName;
    }

    public static String readTicketFromFile(String fileName) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileReader fr = new FileReader(fileName);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
