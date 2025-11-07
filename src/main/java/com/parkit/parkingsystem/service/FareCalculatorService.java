package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

/**
 * Service de calcul des tarifs de stationnement.
 * Gère le calcul des prix selon le type de véhicule, la durée et les remises.
 */
public class FareCalculatorService {

    /**
     * Calcule le tarif de stationnement pour un ticket.
     * Gratuit en dessous de 30 minutes. Applique une remise de 5% si demandée.
     *
     * @param ticket le ticket contenant les informations de stationnement
     * @param discount true pour appliquer la remise de 5%, false sinon
     * @throws IllegalArgumentException si l'heure de sortie est invalide
     */
    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inMillis = ticket.getInTime().getTime();
        long outMillis = ticket.getOutTime().getTime();

        double durationMillis = outMillis - inMillis;
        double durationMinutes = durationMillis / (60 * 1000);
        double durationHours = durationMillis / (60 * 60 * 1000);

        // Gratuité en deça de 30 minutes de parking.
        if (durationMinutes < 30) {
            ticket.setPrice(0);
            return;
        }

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }

        // Réduction de 5% pour les utilisateurs récurrents.
        if (discount) { ticket.setPrice(ticket.getPrice() * 0.95); }
    }

    /**
     * Calcule le tarif de stationnement sans remise.
     *
     * @param ticket le ticket contenant les informations de stationnement
     */
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}