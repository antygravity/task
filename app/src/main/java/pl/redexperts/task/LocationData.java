package pl.redexperts.task;

import org.codehaus.jackson.annotate.JsonProperty;

public class LocationData {

    @JsonProperty("location")
    Location location;

    @JsonProperty("text")
    String text;

    @JsonProperty("image")
    String image;

    public LocationData() {
    }

    public LocationData(Location location, String text, String image) {
        this.location = location;
        this.text = text;
        this.image = image;
    }

    public Location getLocation() {
        return location;
    }

    public String getText() {
        return text;
    }

    public String getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "location=" + location +
                ", text='" + text + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
