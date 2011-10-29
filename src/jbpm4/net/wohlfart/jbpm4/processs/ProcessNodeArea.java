package net.wohlfart.jbpm4.processs;

@Deprecated
public class ProcessNodeArea {

    private final String name;
    private final String coords;

    public ProcessNodeArea(final String name, final String coords) {
        this.name = name;
        this.coords = coords;
    }

    public String getName() {
        return name;
    }

    public String getCoords() {
        return coords;
    }

}
