import java.util.ArrayList;
import java.util.List;

public class Artist {
    final private String name;
    final private List<Album> albums;

    public Artist(String name) {
        this.name = name;
        this.albums = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void addAlbum(Album album) {
        albums.add(album);
    }
}
