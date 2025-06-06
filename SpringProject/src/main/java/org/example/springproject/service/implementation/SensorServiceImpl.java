/**
 * SensorServiceImpl.java
 * It is used to implement the methods from SensorService interface.
 * @author Ilea Robert-Ioan
 */
package org.example.springproject.service.implementation;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.example.springproject.dto.RoomDTO;
import org.example.springproject.dto.SensorDTO;
import org.example.springproject.entity.Details;
import org.example.springproject.entity.Sensor;
import org.example.springproject.service.SensorService;

import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

/**
 * SensorServiceImpl is a service class that implements the SensorService interface.
 * It provides methods to manage sensors in a Firestore database.
 */
@Service
public class SensorServiceImpl implements SensorService {

    /**
     * Firestore instance used to interact with the Firestore database.
     */
    private final Firestore firestore;

    /**
     * The name of the Firestore collection where sensors are stored.
     */
    private static final String SENSOR_COLLECTION = "sensors";

    /**
     * The name of the Firestore collection where rooms are stored.
     */
    private static final String ROOM_COLLECTION = "rooms";

    /**
     * Constructor for SensorServiceImpl.
     *
     * @param firestore Firestore instance used to interact with the database.
     */
    public SensorServiceImpl(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Adds a new sensor to the Firestore database.
     *
     * @param sensor The sensor to be added.
     * @return A SensorDTO containing the details of the added sensor.
     * @throws RuntimeException If an error occurs while adding the sensor.
     */
    @Override
    public SensorDTO addSensor(Sensor sensor) throws RuntimeException {
        try {
            DocumentReference sensorRef = firestore.collection(SENSOR_COLLECTION).document();
            sensorRef.set(sensor).get();
            return new SensorDTO(sensorRef.getId(), sensor.getSensorType(), sensor.getPort(), sensor.getDetails(), sensor.isActive());
        } catch (Exception e) {
            throw new RuntimeException("Error while adding a new sensor: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a sensor by its ID from the Firestore database.
     *
     * @param id The ID of the sensor to be deleted.
     * @return A SensorDTO containing the details of the deleted sensor.
     * @throws RuntimeException If an error occurs while deleting the sensor.
     */
    @Override
    public SensorDTO deleteSensorById(String id) throws RuntimeException {
        try {
            DocumentReference sensorRef = firestore.collection(SENSOR_COLLECTION).document(id);
            DocumentSnapshot sensorSnapshot = sensorRef.get().get();

            if (!sensorSnapshot.exists()) {
                throw new RuntimeException("Sensor with id: " + id + " doesn't exist!");
            }

            Sensor sensor = deserializeSensor(sensorSnapshot);
            sensorRef.delete().get();
            return new SensorDTO(id, sensor.getSensorType(), sensor.getPort(), sensor.getDetails(), sensor.isActive());
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting the sensor: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes a DocumentSnapshot into a Sensor object.
     *
     * @param sensorSnapshot The DocumentSnapshot to be deserialized.
     * @return A Sensor object containing the details from the snapshot.
     * @throws RuntimeException If an error occurs during deserialization.
     */
    public Sensor deserializeSensor(DocumentSnapshot sensorSnapshot) throws RuntimeException {
        try {
            String sensorType = sensorSnapshot.getString("sensorType");
            Long portLong = sensorSnapshot.getLong("port");
            Integer port = (portLong != null) ? portLong.intValue() : 0;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> detailsList = (List<Map<String, Object>>) sensorSnapshot.get("details");
            List<Details> details = new ArrayList<>();
            boolean isActive = Boolean.TRUE.equals(sensorSnapshot.getBoolean("active"));
            checkDetailsList(detailsList, details);
            return new Sensor(sensorType, port, details, isActive);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing sensor: " + e.getMessage(), e);
        }

    }

    /**
     * Updates an existing sensor in the Firestore database.
     *
     * @param id            The ID of the sensor to be updated.
     * @param updatedSensor The updated sensor object.
     * @return A SensorDTO containing the details of the updated sensor.
     * @throws RuntimeException If an error occurs while updating the sensor.
     */
    @Override
    public SensorDTO updateSensor(String id, Sensor updatedSensor) throws RuntimeException {
        try {
            DocumentReference sensorRef = firestore.collection(SENSOR_COLLECTION).document(id);
            DocumentSnapshot sensorSnapshot = sensorRef.get().get();

            if (!sensorSnapshot.exists()) {
                throw new RuntimeException("Sensor with id: " + id + "doesn't exist!");
            }

            sensorRef.set(updatedSensor).get();
            return new SensorDTO(id, updatedSensor.getSensorType(), updatedSensor.getPort(), updatedSensor.getDetails(), updatedSensor.isActive());
        } catch (Exception e) {
            throw new RuntimeException("Error while updating the sensor: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all sensors from the Firestore database.
     *
     * @return A list of SensorDTO objects containing the details of all sensors.
     * @throws RuntimeException If an error occurs while fetching the sensors.
     */
    @Override
    public List<SensorDTO> getSensors() throws RuntimeException {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(SENSOR_COLLECTION).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<SensorDTO> sensors = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                String id = document.getId();
                Sensor sensor = deserializeSensor(document);
                sensors.add(new SensorDTO(id, sensor.getSensorType(), sensor.getPort(), sensor.getDetails(), sensor.isActive()));
            }
            return sensors;
        } catch (Exception e) {
            throw new RuntimeException("Error while fetching the sensors: " + e.getMessage(), e);
        }
    }

    /**
     * Saves new sensor data to the Firestore database.
     *
     * @param newSensorData The new sensor data to be saved.
     * @return A string indicating the update time of the saved sensor data.
     * @throws RuntimeException If an error occurs while saving the sensor data.
     */
    @Override
    public String saveSensorData(SensorDTO newSensorData) throws RuntimeException {
        try {
            DocumentReference docRef = firestore.collection(SENSOR_COLLECTION).document(newSensorData.getId());

            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot documentSnapshot = future.get();

            List<Details> detailsList = new ArrayList<>();

            if (documentSnapshot.exists()) {
                SensorDTO existingSensor = documentSnapshot.toObject(SensorDTO.class);
                if (existingSensor != null && existingSensor.getDetails() != null) {
                    detailsList = existingSensor.getDetails();
                }
            }

            detailsList.add(newSensorData.getDetails().get(0));
            SensorDTO updatedSensor = new SensorDTO(
                    newSensorData.getId(),
                    newSensorData.getSensorType(),
                    newSensorData.getPort(),
                    detailsList,
                    newSensorData.isActive()
            );

            WriteResult result = docRef.set(updatedSensor).get();
            return result.getUpdateTime().toString();
        } catch (Exception e) {
            throw new RuntimeException("Error while saving sensor data: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a sensor by its ID from the Firestore database.
     *
     * @param sensorId The ID of the sensor to be retrieved.
     * @return A SensorDTO containing the details of the retrieved sensor.
     * @throws RuntimeException If an error occurs while fetching the sensor.
     */
    @Override
    public SensorDTO getSensorById(String sensorId) throws RuntimeException {
        try {
            DocumentReference sensorRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
            ApiFuture<DocumentSnapshot> future = sensorRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                throw new RuntimeException("Sensor with id: " + sensorId + "doesn't exist!");
            }

            Sensor sensor = deserializeSensor(document);
            if (sensor == null) {
                throw new RuntimeException("Deserialization failed: sensor is null");
            }
            return new SensorDTO(sensorId, sensor.getSensorType(), sensor.getPort(), sensor.getDetails(), sensor.isActive());
        } catch (Exception e) {
            throw new RuntimeException("Error while fetching sensor by id: " + e.getMessage(), e);
        }
    }

    /**
     * Checks the details list and deserializes it into a list of Details objects.
     *
     * @param detailsList The list of details to be checked and deserialized.
     * @param details     The list to which the deserialized Details objects will be added.
     * @throws RuntimeException If an error occurs while processing the details list.
     */
    private static void checkDetailsList(List<Map<String, Object>> detailsList, List<Details> details) throws RuntimeException {
        if (detailsList != null) {
            try {
                for (Map<String, Object> detailMap : detailsList) {
                    Timestamp timestampObj = (Timestamp) detailMap.get("timestamp");
                    Map<String, Float> data = deserializeDetailsList(detailMap);
                    details.add(new Details(timestampObj, data));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error processing details: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Deserializes a map of details into a map of String and Float.
     *
     * @param detailMap The map containing the details to be deserialized.
     * @return A map containing the deserialized details.
     */
    private static Map<String, Float> deserializeDetailsList(Map<String, Object> detailMap) throws RuntimeException {
        try {
            Map<String, Float> data = new HashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) detailMap.get("data");

            if (dataMap != null) {
                for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                    data.put(entry.getKey(), ((Number) entry.getValue()).floatValue());
                }
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing details list: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves sensor data for a specific date.
     *
     * @param sensorId     The ID of the sensor.
     * @param selectedDate The date for which to retrieve the sensor data.
     * @return A list of Details objects containing the sensor data for the specified date.
     * @throws RuntimeException If an error occurs while fetching the sensor data.
     */
    @Override
    public List<Details> getSensorDataByDate(String sensorId, Date selectedDate) throws RuntimeException {
        try {
            DocumentReference docRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
            DocumentSnapshot snapshot = docRef.get().get();

            if (!snapshot.exists()) {
                throw new RuntimeException("Sensor with id: " + sensorId + "doesn't exist!");
            }

            SensorDTO sensorDTO = snapshot.toObject(SensorDTO.class);

            if (sensorDTO == null || sensorDTO.getDetails() == null) {
                return new ArrayList<>();
            }

            Calendar calSelected = Calendar.getInstance();
            calSelected.setTime(selectedDate);

            Calendar calDetail = Calendar.getInstance();

            return sensorDTO.getDetails().stream()
                    .filter(detail -> {
                        if (detail.getTimestamp() == null) {
                            return false;
                        }
                        calDetail.setTimeInMillis(detail.getTimestamp().getSeconds() * 1000);
                        return calSelected.get(Calendar.YEAR) == calDetail.get(Calendar.YEAR) &&
                                calSelected.get(Calendar.MONTH) == calDetail.get(Calendar.MONTH) &&
                                calSelected.get(Calendar.DAY_OF_MONTH) == calDetail.get(Calendar.DAY_OF_MONTH);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sensor data for date: " + selectedDate, e);
        }
    }

    /**
     * Retrieves the last detail for a specific sensor.
     *
     * @param sensorId The ID of the sensor.
     * @return The last Details object for the specified sensor, or null if no details are found.
     * @throws RuntimeException If an error occurs while fetching the last detail.
     */
    @Override
    public Details getLastDetailForSensor(String sensorId) throws RuntimeException {
        try {
            DocumentReference docRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
            DocumentSnapshot snapshot = docRef.get().get();

            if (!snapshot.exists()) {
                throw new RuntimeException("Sensor with id: " + sensorId + " doesn't exist!");
            }

            SensorDTO sensorDTO = snapshot.toObject(SensorDTO.class);

            if (sensorDTO == null || sensorDTO.getDetails() == null || sensorDTO.getDetails().isEmpty()) {
                System.out.println("No details found for sensor with id: " + sensorId);
                return null;
            }

            return sensorDTO.getDetails().stream()
                    .filter(d -> d.getTimestamp() != null)
                    .max(Comparator.comparing(d -> d.getTimestamp().getSeconds()))
                    .orElse(null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch last detail for sensor with id: " + sensorId, e);
        }
    }

    /**
     * Clears all sensor details for a specific sensor ID.
     * @param sensorId The ID of the sensor whose details are to be cleared.
     * @throws RuntimeException If an error occurs while clearing the sensor details.
     */
    @Override
    public void clearAllSensorsDetailsBySensorId(String sensorId) throws RuntimeException {
        try {
            DocumentReference sensorRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
            DocumentSnapshot sensorSnapshot = sensorRef.get().get();

            if (!sensorSnapshot.exists()) {
                throw new RuntimeException("Sensor with id: " + sensorId + " doesn't exist!");
            }

            Sensor sensor = sensorSnapshot.toObject(Sensor.class);
            if (sensor != null) {
                sensor.setDetails(new ArrayList<>());
                sensorRef.set(sensor).get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while clearing all details for sensor with id: " + sensorId, e);
        }
    }

    /**
     * Clears sensor details from a specific room.
     * @param roomId The ID of the room from which to clear sensor details.
     * @throws RuntimeException If an error occurs while clearing sensor details from the room.
     */
    @Override
    public void clearSensorDetailsFromRoom(String roomId) throws RuntimeException{
        try{
            DocumentReference roomRef = firestore.collection(ROOM_COLLECTION).document(roomId);

            DocumentSnapshot roomSnapshot = roomRef.get().get();

            if (!roomSnapshot.exists()) {
                throw new RuntimeException("Room with id: " + roomId + " doesn't exist!");
            }

            Map<String, Object> data = roomSnapshot.getData();

            if(data != null && data.containsKey("sensors")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sensors = (List<Map<String, Object>>) data.get("sensors");

                for (Map<String, Object> sensor : sensors) {
                    sensor.put("details", new ArrayList<>());
                }

                data.put("sensors", sensors);
                roomRef.set(data).get();
            }
        }catch (Exception e){
            throw new RuntimeException("Error while clearing sensor details from room: " + e.getMessage(), e);
        }
    }

    /**
     * Activates the selected sensors by their IDs.
     * @param sensorIds A list of sensor IDs to be activated.
     * @throws RuntimeException If an error occurs while activating the sensors.
     */
    @Override
    public void activateSelectedSensor(List<String> sensorIds) throws RuntimeException {
        try {
            // Update in SENSOR_COLLECTION
            for (String sensorId : sensorIds) {
                DocumentReference docRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
                docRef.update("active", true).get();
            }

            // Update also in ROOMS collection
            for (String sensorId : sensorIds) {
                Query query = firestore.collection("rooms").whereArrayContains("sensorsIds", sensorId);

                ApiFuture<QuerySnapshot> future = query.get();
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();

                for (QueryDocumentSnapshot roomDoc : documents) {
                    RoomDTO room = roomDoc.toObject(RoomDTO.class);
                    boolean updated = false;

                    if (room != null && room.getSensors() != null) {
                        for (SensorDTO sensor : room.getSensors()) {
                            if (sensor != null && sensorId.equals(sensor.getId())) {
                                sensor.setActive(true);
                                updated = true;
                            }
                        }
                    }

                    if (updated) {
                        firestore.collection("rooms").document(roomDoc.getId()).set(room).get();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while activating sensors: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivates the selected sensors by their IDs.
     * @param sensorIds A list of sensor IDs to be deactivated.
     * @throws RuntimeException If an error occurs while deactivating the sensors.
     */
    @Override
    public void deactivateSelectedSensor(List<String> sensorIds) throws RuntimeException {
        try {
            // Deactivate sensors in the SENSOR_COLLECTION
            for (String sensorId : sensorIds) {
                DocumentReference docRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
                docRef.update("active", false).get();
            }

            // Deactivate sensors in rooms
            ApiFuture<QuerySnapshot> futureRooms = firestore.collection(ROOM_COLLECTION).get();
            List<QueryDocumentSnapshot> roomDocuments = futureRooms.get().getDocuments();

            for (QueryDocumentSnapshot roomDoc : roomDocuments) {
                RoomDTO room = roomDoc.toObject(RoomDTO.class);
                boolean updated = false;

                if (room != null && room.getSensors() != null) {
                    for (SensorDTO sensor : room.getSensors()) {
                        if (sensor != null && sensorIds.contains(sensor.getId())) {
                            sensor.setActive(false);
                            updated = true;
                        }
                    }

                    // Save the updated room only if any sensor was deactivated
                    if (updated) {
                        firestore.collection(ROOM_COLLECTION).document(roomDoc.getId()).set(room).get();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while deactivating sensors: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the status of a sensor by its ID.
     *
     * @param sensorId The ID of the sensor.
     * @param status   The status to set (true for active, false for inactive).
     * @throws RuntimeException If an error occurs while setting the sensor status.
     */
    @Override
    public void setStatusForSensor(String sensorId, boolean status) throws RuntimeException {
        try {
            DocumentReference sensorRef = firestore.collection(SENSOR_COLLECTION).document(sensorId);
            sensorRef.update("active", status).get();

            // Update the status in the ROOMS collection as well
            // Only one room can have the sensor, so we can use a simple query
            ApiFuture<QuerySnapshot> future = firestore.collection(ROOM_COLLECTION).get();
            List<QueryDocumentSnapshot> roomDocs = future.get().getDocuments();
            for (QueryDocumentSnapshot roomDoc : roomDocs) {
                RoomDTO room = roomDoc.toObject(RoomDTO.class);
                if (room != null && room.getSensors() != null) {
                    for (SensorDTO sensor : room.getSensors()) {
                        if (sensor.getId().equals(sensorId)) {
                            sensor.setActive(status);
                            firestore.collection(ROOM_COLLECTION).document(roomDoc.getId()).update("sensors", room.getSensors()).get();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
