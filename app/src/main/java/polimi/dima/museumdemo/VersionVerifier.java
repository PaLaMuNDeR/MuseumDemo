package polimi.dima.museumdemo;

public class VersionVerifier {
    int id;
    int version;


    public VersionVerifier(){}

    public VersionVerifier(int id, int version) {
        this.id = id;
        this.version=version;
    }

    public VersionVerifier(int version){
        this.version = version;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}