package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
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
        validateTimes(ticket);

        double durationHours = getDurationInHours(ticket);

        if (durationHours * 60 < 30) {
            ticket.setPrice(0);
            return;
        }

        double price = calculateBasePrice(durationHours, ticket.getParkingSpot().getParkingType());

        if (discount) {
            price = price * 0.95;
        }

        ticket.setPrice(roundToTwoDecimals(price));
    }

    /**
     * Calcule le tarif de stationnement sans remise.
     *
     * @param ticket le ticket contenant les informations de stationnement
     */
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    /**
     * Vérifie la validité des heures d'entrée et de sortie du ticket.
     * Déclenche une exception si l'heure de sortie est absente ou antérieure à l'heure d'entrée.
     *
     * @param ticket le ticket à vérifier
     * @throws IllegalArgumentException si les heures sont incorrectes
     */
    private void validateTimes(Ticket ticket) {
        if (ticket.getOutTime() == null || ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
        }
    }

    /**
     * Calcule la durée totale du stationnement en heures.
     * La durée est obtenue en millisecondes puis convertie en heures.
     *
     * @param ticket le ticket contenant les heures d'entrée et de sortie
     * @return la durée de stationnement en heures
     */
    private double getDurationInHours(Ticket ticket) {
        double durationMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        return durationMillis / (60 * 60 * 1000);
    }

    /**
     * Calcule le prix brut du stationnement selon la durée et le type de véhicule.
     * Les tarifs horaires sont définis dans la classe Fare.
     *
     * @param hours la durée totale de stationnement en heures
     * @param parkingType le type de véhicule (CAR, BIKE...)
     * @return le prix calculé avant réduction
     * @throws IllegalArgumentException si le type de véhicule est inconnu
     */
    private double calculateBasePrice(double hours, ParkingType parkingType){
        switch (parkingType) {
            case CAR :
                return hours * Fare.CAR_RATE_PER_HOUR;
            case BIKE :
                return hours * Fare.BIKE_RATE_PER_HOUR;
            default :
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }

    /**
     * Arrondit un prix au centième (2 décimales).
     *
     * @param value le prix à arrondir
     * @return le prix arrondi au centième
     */
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}