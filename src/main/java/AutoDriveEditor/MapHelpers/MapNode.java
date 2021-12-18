package AutoDriveEditor.MapHelpers;

import java.util.LinkedList;

public class MapNode {

    public LinkedList<MapNode> incoming;
    public LinkedList<MapNode> outgoing;
    public double x, y, z;
    public int id, flag;
    public boolean selected;
    public boolean controlPoint;

    public MapNode(int id, double x, double y, double z, int flag, boolean selected, boolean controlPoint) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.incoming = new LinkedList<>();
        this.outgoing = new LinkedList<>();
        this.flag = flag;

        // editor use only!

        this.selected = selected;
        this.controlPoint = controlPoint;
    }



}