import java.util.ArrayList;
import java.util.List;

public class Album {
    final private String title;
    final private Artist artist;
    final private List<Track> tracks;

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

    // May not apply to android - for only adding track
    public void addTrack(Track track) {
        tracks.add(track);
    }
}
