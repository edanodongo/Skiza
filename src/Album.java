import java.util.ArrayList;
import java.util.List;

public class Album {
    private String title;
    private Artist artist;
    private List<Track> tracks;

    public Album(String title, Artist artist) {
        this.title = title;
        this.artist = artist;
        this.tracks = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public Artist getArtist() {
        return artist;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }
}
