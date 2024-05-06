package com.simu;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.google.gson.JsonArray;

// import org.json.JSONArray;
// import org.json.JSONObject;

import com.simu.service.Api;

public class Main {
    // api bdd coté réel
    private static final String SENSOR_API_URL_EVENT = "http://localhost:3000/api/event";

    // api bdd coté simu
    private static final String EVENT_API_URL_EVENT_UPDATE = "http://localhost:4000/api/event";
    private static final String SENSOR_API_URL_VEHICLE_ON_EVENT = "http://localhost:4000/api/vehicle/event";
    private static final String SENSOR_ALL_API_URL_EVENT_TOSTOP = "http://localhost:4000/api/event/tostop";

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
    private static final Api api = new Api();

    public static void main(String[] args) throws Exception {
        String json;
        JSONArray jsonArray;

        while (true) {
            try {
                // Récupérer la liste des feux actif
                json = Api.sendGetRequest(SENSOR_API_URL_EVENT);
                jsonArray = new JSONArray(json);

                // on boucle pour chaque event (event = feu actif)
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject trafficLight = jsonArray.getJSONObject(i);
                    int eventId = trafficLight.getInt("eventId");

                    // on récupère l'information du nombre de véhicules sur le site
                    String string = Api.sendGetRequest(SENSOR_API_URL_VEHICLE_ON_EVENT + "?eventId=" + eventId);
                    JSONArray vehiclesOnSite_jsonarray = new JSONArray(string);
                    int vehiclesOnSite = vehiclesOnSite_jsonarray.getJSONObject(0).getInt("vehiclesOnSite");

                    // on crée l'objet details qui sera envoyé au thread
                    JSONObject details = new JSONObject();
                    details.put("id", trafficLight.getInt("id"));
                    details.put("intensity", trafficLight.getInt("intensity"));
                    details.put("latitude", trafficLight.getDouble("latitude"));
                    details.put("longitude", trafficLight.getDouble("longitude"));

                    // on lance le bon thread en fonction du nombre de véhicules sur le site
                    Thread thread = (vehiclesOnSite > 0) ? new DecreaseIntensity(details, vehiclesOnSite)
                            : new IncreaseIntensity(details);
                    thread.start();
                }
                Thread.sleep(5000); // 5s avant le prochain update
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class DecreaseIntensity extends Thread {
        private JSONObject jsonObject;
        private int nombreVehicules;

        public DecreaseIntensity(JSONObject jsonObject, int nombreVehicules) {
            this.jsonObject = jsonObject;
            this.nombreVehicules = nombreVehicules;
        }

        @Override
        public void run() {
            // Synchronisation de l'objet jsonObject pour assurer la sécurité des threads
            synchronized (jsonObject) {
                boolean modif = false;
                int currentIntensity = jsonObject.getInt("intensity");
                int eventId = jsonObject.getInt("id");

                /*
                 * baisse son intensité si l'intensité est entre 0 et 3 et si le nombre de
                 * véhicules de pompier sur place est supérieur à 0
                 */
                if (currentIntensity > 0 && currentIntensity < 4 && nombreVehicules > 0) {
                    jsonObject.put("intensity", --currentIntensity);
                    modif = true;
                }
                /*
                 * baise en intensité si l'intensité est supérieur à 4 et qu'il y a 2 véhcilue
                 * de pompier sur place
                 */
                else if (currentIntensity >= 4 && nombreVehicules < 2) {
                    jsonObject.put("intensity", --currentIntensity);
                    modif = true;
                }
                /*
                 * déclare l'event terminé si le feu est éteint et lance le retour a la base
                 * pour les véhicules de pompiers
                 */
                else {
                    try {
                        Api.sendGetRequest(SENSOR_ALL_API_URL_EVENT_TOSTOP + "?eventId=" + eventId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // on modifie au niveau de la bdd l'intensité si une modififaction a été fait
                if (modif == true) {
                    try {
                        Api.sendPutRequest(EVENT_API_URL_EVENT_UPDATE, jsonObject.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class IncreaseIntensity extends Thread {
        private JSONObject jsonObject;

        public IncreaseIntensity(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            // Synchronisation de l'objet jsonObject pour assurer la sécurité des threads
            synchronized (jsonObject) {
                int currentIntensity = jsonObject.getInt("intensity");
                // on vérifie que le feu ne soit pas déjà au max de l'intensité
                if (currentIntensity < 10) { // si ce c'est ce n'est pas le cas on augmente son intensité
                    jsonObject.put("intensity", ++currentIntensity);
                    try {
                        Api.sendPutRequest(EVENT_API_URL_EVENT_UPDATE, jsonObject.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
