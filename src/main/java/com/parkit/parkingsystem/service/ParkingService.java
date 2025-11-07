package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 * La classe ParkingService gère les opérations principales associées au système de parking.
 * Elle s'occupe des entrées et sorties de véhicules ainsi que des interactions avec les services
 * sous-jacents tels que le calcul des tarifs, la lecture des entrées et la persistance des données
 * pour les places de stationnement et les tickets.
 */
public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private FareCalculatorService fareCalculatorService;
    private InputReaderUtil inputReaderUtil;
    private ParkingSpotDAO parkingSpotDAO;
    private TicketDAO ticketDAO;

    /**
     * Constructeur initialisant les dépendances du service.
     *
     * @param fareCalculatorService service de calcul des tarifs
     * @param inputReaderUtil utilitaire de lecture des entrées utilisateur
     * @param parkingSpotDAO DAO pour la gestion des places de parking
     * @param ticketDAO DAO pour la gestion des tickets
     */
    public ParkingService(FareCalculatorService fareCalculatorService, InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO) {
        this.fareCalculatorService = fareCalculatorService;
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    /**
     * Traite l'entrée d'un véhicule dans le parking.
     * Attribue une place, crée et enregistre un ticket.
     * Affiche un message de bienvenue pour les utilisateurs récurrents.
     *
     * @return void
     */
    public void processIncomingVehicle() {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if (parkingSpot != null && parkingSpot.getId() > 0) {
                String vehicleRegNumber = getVehichleRegNumber();
                parkingSpot.setAvailable(false);
                parkingSpotDAO.updateParking(parkingSpot);//allot this parking space and mark its availability as false

                // Si utilisateur régulier, message personnalisé annonçant la remise 5%.
                if (ticketDAO.getNbTicket(vehicleRegNumber) > 0) {
                    System.out.println("Heureux de vous revoir ! En tant qu'utilisateur régulier de notre parking," +
                            " vous allez obtenir une remise de 5%.");
                }

                Date inTime = new Date();
                Ticket ticket = new Ticket();
                //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
                //ticket.setId(ticketID);
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(0);
                ticket.setInTime(inTime);
                ticket.setOutTime(null);
                ticketDAO.saveTicket(ticket);
                System.out.println("Generated Ticket and saved in DB");
                System.out.println("Please park your vehicle in spot number:" + parkingSpot.getId());
                System.out.println("Recorded in-time for vehicle number:" + vehicleRegNumber + " is:" + inTime);
            }
        } catch (Exception e) {
            logger.error("Unable to process incoming vehicle", e);
        }
    }

    /**
     * Récupère le numéro d'immatriculation du véhicule saisi par l'utilisateur.
     *
     * @return String le numéro d'immatriculation
     * @throws Exception si la lecture échoue
     */
    private String getVehichleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    /**
     * Récupère la prochaine place de parking disponible selon le type de véhicule.
     *
     * @return ParkingSpot la place disponible, ou null si aucune n'est trouvée
     */
    public ParkingSpot getNextParkingNumberIfAvailable() {
        int parkingNumber = 0;
        ParkingSpot parkingSpot = null;
        try {
            ParkingType parkingType = getVehichleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if (parkingNumber > 0) {
                parkingSpot = new ParkingSpot(parkingNumber, parkingType, true);
            } else {
                throw new Exception("Error fetching parking number from DB. Parking slots might be full");
            }
        } catch (IllegalArgumentException ie) {
            logger.error("Error parsing user input for type of vehicle", ie);
        } catch (Exception e) {
            logger.error("Error fetching next available parking slot", e);
        }
        return parkingSpot;
    }

    /**
     * Demande à l'utilisateur de sélectionner le type de véhicule (voiture ou moto).
     *
     * @return ParkingType le type de véhicule sélectionné
     * @throws IllegalArgumentException si la saisie est invalide
     */
    private ParkingType getVehichleType() {
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch (input) {
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    /**
     * Traite la sortie d'un véhicule du parking.
     * Calcule le tarif, applique les remises éventuelles et libère la place.
     *
     * @return void
     */
    public void processExitingVehicle() {
        try {
            String vehicleRegNumber = getVehichleRegNumber();
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            Date outTime = new Date();
            ticket.setOutTime(outTime);

            // Condition permettant d'appliquer la remise à la sortie du véhicule.
            if (ticketDAO.getNbTicket(vehicleRegNumber) > 1) {
                fareCalculatorService.calculateFare(ticket, true);
            } else {
                fareCalculatorService.calculateFare(ticket);
            }

            boolean ticketUpdated = ticketDAO.updateTicket(ticket);
            logger.info("Ticket update result for vehicle {}: {}", vehicleRegNumber, ticketUpdated);

            if (ticketUpdated) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                parkingSpotDAO.updateParking(parkingSpot);
                System.out.println("Please pay the parking fare:" + ticket.getPrice());
                System.out.println("Recorded out-time for vehicle number:" + ticket.getVehicleRegNumber() + " is:" + outTime);
            } else {
                System.out.println("Unable to update ticket information. Error occurred");
            }
        } catch (Exception e) {
            logger.error("Unable to process exiting vehicle", e);
        }
    }
}
