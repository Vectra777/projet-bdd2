package com.project.artconnect.model;

import java.time.LocalDateTime;

public class Booking {
    private Workshop workshop;
    private CommunityMember member;
    private LocalDateTime bookingDate;
    private String paymentStatus; // PENDING, PAID, CANCELLED

    public Booking() {
    }

    public Booking(Workshop workshop, CommunityMember member) {
        this.workshop = workshop;
        this.member = member;
        this.bookingDate = LocalDateTime.now();
        this.paymentStatus = "PENDING";
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public CommunityMember getMember() {
        return member;
    }

    public void setMember(CommunityMember member) {
        this.member = member;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
