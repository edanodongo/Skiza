
public class Track {
    final private String id;
    final private String title;
    final private Artist artist;
    final private Album album;
    final private String filePath;
    final private boolean isFavorite;
    final private long duration;
    final private String albumArtPath; // Path to the album art

    public Track(String id, String title, Artist artist, Album album, String filePath, String albumArtPath, boolean isFavorite, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.albumArtPath = albumArtPath;
        this.isFavorite = isFavorite;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Artist getArtist() {
        return artist;
    }

    public Album getAlbum() {
        return album;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public long getDuration() {
        return duration;
    }

    public String getAlbumArtPath() {
        return albumArtPath;
    }
}

