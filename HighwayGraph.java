
/**
 * An undirected, adjacency-list based graph data structure developed
 * specifically for METAL highway mapping graphs.
 * 
 * Starter implementation for the METAL Learning Module
 * Working with METAL Data
 * 
 * @author Jim Teresco ADD LAB PARTNER NAMES HERE
 * @version January 2024
 */

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Scanner;

public class HighwayGraph
{

    private static final DecimalFormat df = new DecimalFormat("#.###");

    // Small, internal data structure representing a
    // latitude-longitude pair.  It has the added benefit
    // of being able to compute its distance to another
    // LatLng object.
    private class LatLng {
        private double lat, lng;
        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        /**
        compute the distance in miles from this LatLng to another

        @param other another LatLng
        @return the distance in miles from this LatLng to other
         */
        public double distanceTo(LatLng other) {
            /** radius of the Earth in statute miles */
            final double EARTH_RADIUS = 3963.1;

            // did we get the same point?
            if (equals(other)) return 0.0;

            // coordinates in radians
            double rlat1 = Math.toRadians(lat);
            double rlng1 = Math.toRadians(lng);
            double rlat2 = Math.toRadians(other.lat);
            double rlng2 = Math.toRadians(other.lng);

            return Math.acos(Math.cos(rlat1)*Math.cos(rlng1)*Math.cos(rlat2)*Math.cos(rlng2) +
                Math.cos(rlat1)*Math.sin(rlng1)*Math.cos(rlat2)*Math.sin(rlng2) +
                Math.sin(rlat1)*Math.sin(rlat2)) * EARTH_RADIUS;
        }

        /**
        Compare another LatLng with this for equality, subject to the
        specified tolerance.

        @param o the other LatLng
        @pre o instanceof LatLng
        @return whether the two lat/lng pairs should be considered equal
         */
        public boolean equals(Object o) {
            final double TOLERANCE = 0.00001;
            LatLng other = (LatLng)o;

            return ((Math.abs(other.lat-lat) < TOLERANCE) &&
                (Math.abs(other.lng-lng) < TOLERANCE));
        }

        public String toString() {
            return "(" + lat + "," + lng + ")";
        }
    }

    // our private internal structure for a Vertex
    private class Vertex {
        private String label;
        private LatLng point;
        private Edge head;

        public Vertex(String l, double lat, double lng) {
            label = l;
            point = new LatLng(lat,lng);
        }

    }

    // our private internal structure for an Edge
    private class Edge {

        // the edge needs to know its own label, its destination vertex (note that
        // it knows its source as which vertex's list contains this edge), an
        // optional array of points that improve the edge's shape, and its length
        // in miles, which is computed on construction
        private String label;
        private int dest;
        private LatLng[] shapePoints;
        private double length;

        // and Edge is also a linked list
        private Edge next;

        public Edge(String l, int dst, LatLng startPoint, LatLng points[], LatLng endPoint, Edge n) {
            label = l;
            dest = dst;
            shapePoints = points;
            next = n;
            length = 0.0;
            LatLng prevPoint = startPoint;
            if (points != null) {
              //  if(next > dst)
                for (int pointNum = 0; pointNum < points.length; pointNum++) {
                    length += prevPoint.distanceTo(points[pointNum]);
                    prevPoint = points[pointNum];
                }
            }
            length += prevPoint.distanceTo(endPoint);
        }
    }

    // vertices -- we know how many at the start, so these 
    // are simply in an array
    private Vertex[] vertices;

    // number of edges
    private int numEdges;

    // construct from a TMG format file that comes from the given
    // Scanner (likely over a File or URLConnection, but does not
    // matter here)
    public HighwayGraph(Scanner s) {

        // read header line -- for now assume it's OK, but should
        // check
        s.nextLine();

        // read number of vertices and edges
        int numVertices = s.nextInt();
        numEdges = s.nextInt();

        // construct our array of Vertices
        vertices = new Vertex[numVertices];

        // next numVertices lines are Vertex entries
        for (int vNum = 0; vNum < numVertices; vNum++) {
            vertices[vNum] = new Vertex(s.next(), s.nextDouble(), s.nextDouble());
        }

        // next numEdge lines are Edge entries
        for (int eNum = 0; eNum < numEdges; eNum++) {
            int v1 = s.nextInt();
            int v2 = s.nextInt();
            String label = s.next();
            // shape points take us to the end of the line, and this
            // will be just a new line char if there are none for this edge
            String shapePointText = s.nextLine().trim();
            String[] shapePointStrings = shapePointText.split(" ");
            LatLng v1Tov2[] = null;
            LatLng v2Tov1[] = null;
            if (shapePointStrings.length > 1) {
                // build arrays in both orders
                v1Tov2 = new LatLng[shapePointStrings.length/2];
                v2Tov1 = new LatLng[shapePointStrings.length/2];
                for (int pointNum = 0; pointNum < shapePointStrings.length/2; pointNum++) {
                    LatLng point = new LatLng(Double.parseDouble(shapePointStrings[pointNum*2]),
                            Double.parseDouble(shapePointStrings[pointNum*2+1]));
                    v1Tov2[pointNum] = point;
                    v2Tov1[shapePointStrings.length/2 - pointNum - 1] = point;
                }
            }

            // build our Edge structures and add to each adjacency list
            vertices[v1].head = new Edge(label, v2, vertices[v1].point, v1Tov2, vertices[v2].point, vertices[v1].head);
            vertices[v2].head = new Edge(label, v1, vertices[v2].point, v2Tov1, vertices[v1].point, vertices[v2].head);
        }
    }

    // construct and return a human-readable summary of the graph
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("|V|=" + vertices.length + ", |E|=" + numEdges + "\n");
        for (Vertex v : vertices) {
            s.append(v.label + " " + v.point + "\n");
            Edge e = v.head;
            while (e != null) {
                Vertex o = vertices[e.dest];
                s.append("  to " + o.label + " " + o.point + " on " + e.label);
                if (e.shapePoints != null) {
                    s.append(" via");
                    for (int pointNum = 0; pointNum < e.shapePoints.length; pointNum++) {
                        s.append(" " + e.shapePoints[pointNum]);
                    }
                }
                s.append(" length " + df.format(e.length) + "\n");
            }
        }

        return s.toString();
    }

    // try it out
    public static void main(String args[]) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java HighwayGraph tmgfile");
            System.exit(1);
        }

        // read in the file to construct the graph
        Scanner s = new Scanner(new File(args[0]));
        HighwayGraph g = new HighwayGraph(s);
        s.close();

        // print summary of the graph
        System.out.println(g);

	    // ADD CODE HERE TO COMPLETE LAB TASKS

        //Code to find directional extremes and shortest/longest vertex labels
        Vertex north = g.vertices[0];
        Vertex south = g.vertices[0];
        Vertex east = g.vertices[0];
        Vertex west = g.vertices[0];
        Vertex shortest = g.vertices[0];
        Vertex longest = g.vertices[0];

        for (Vertex v : g.vertices)
        {
            if (v.point.lat > north.point.lat)
            {
                north = v;
            }
            if (v.point.lat < south.point.lat)
            {
                south = v;
            }
            if (v.point.lng > east.point.lng)
            {
                east = v;
            }
            if (v.point.lng < west.point.lng)
            {
                west = v;
            }
            if (v.label.length() < shortest.label.length())
            {
                shortest = v;
            }
            if (v.label.length() > longest.label.length())
            {
                longest = v;
            }
        }
        //Print each extreme
        System.out.println("North Extreme: " + north.label);
        System.out.println("South Extreme: " + south.label);
        System.out.println("East Extreme: " + east.label);
        System.out.println("West Extreme: " + west.label);
        System.out.println("Shortest Label: " + shortest.label);
        System.out.println("Longest label: " + longest.label);

        //Code to find shortest/longest edge labels and shortest/longest edge lengths
        Edge shortestLength = g.vertices[0].head;
        Edge longestLength = g.vertices[0].head;
        Edge shortestLabel = g.vertices[0].head;
        Edge longestLabel = g.vertices[0].head;
        double totalEdgeLength = shortestLength.length;
        int edgesVisited = 1;

        for (int i = 1; i < g.vertices.length; i++)
        {
            Edge e = g.vertices[i].head;
            if (e.label.length() < shortestLabel.label.length())
            {
                shortestLabel = e;
            }
            if (e.label.length() > longestLabel.label.length())
            {
                longestLabel = e;
            }
            if (e.length > longestLength.length)
            {
                longestLength = e;
            }
            if (e.length < shortestLength.length)
            {
                shortestLength = e;
            }
            totalEdgeLength += e.length;
            edgesVisited++;

            //------VVVVV----------Edges Visited 02/18/25-----VVVVVV-----------


        Scanner t = new Scanner(new File(args[0]));
        HighwayGraph h = new HighwayGraph(t);
        t.close();
        // print summary of the graph
        System.out.println(h);
        


      // Initialize shortestLength and longestLength with the first edge

// Loop through all vertices and edges to find the shortest and longest edges
int[] edgeName = new int[g.numEdges];
int countEdgeLength = 0;

for (Vertex v : g.vertices) {//
   //This gives us our first edge for the vertex
   e = v.head;
   int nextID = 0;

   while (e != null) {
	for (int j = 0; j <= nextID; j++){
		if (e.dest == edgeName[j]){
			return;
		}
	edgeName[i] = e.dest;
	nextID++;
// Go through the array of ints to see if it has been added yet, if not then add it and go through if statements

       	if (e.length > longestLength.length) {
           longestLength = e;
       }
       	if (e.length < shortestLength.length) {
           shortestLength = e;
       }
       	totalEdgeLength += e.length;
       	edgesVisited++;
       }
       countEdgeLength += e.dest;
       e = e.next;
   }
}


        }

        //Print each Edge extreme case
        System.out.println("Shortest Edge Label: " + shortestLabel.label);
        System.out.println("Longest Edge Label: " + longestLabel.label);
        System.out.println("Shortest Edge Length: " + shortestLength.label + ": " + shortestLength.length);
        System.out.println("Longest Edge Length: " + longestLength.label + ": " + longestLength.length);
        System.out.println("Total Length of all Edges: " + totalEdgeLength);
        System.out.println("Edges Visited: " + edgesVisited);


    }
}
