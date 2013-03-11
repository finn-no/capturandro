package no.finn.capturandro.asynctask;

public class FileObject {
    private String url;
    private String filename;

    public FileObject(String filename, String url) {
        this.filename = filename;
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

}
