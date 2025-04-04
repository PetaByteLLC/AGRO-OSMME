package de.blau.android.contract;

public final class MimeTypes {

    // types and subtypes
    public static final String IMAGE_TYPE                = "image";
    public static final String PNG_SUBTYPE               = "png";
    public static final String BMP_SUBTYPE               = "bmp";
    public static final String APPLICATION_TYPE          = "application";
    public static final String JSON_SUBTYPE              = "json";
    public static final String WMS_EXCEPTION_XML_SUBTYPE = "vnd.ogc.se_xml";
    public static final String TEXT_TYPE                 = "text";
    public static final String MVT_SUBTYPE               = "vnd.mapbox-vector-tile";
    public static final String X_PROTOBUF_SUBTYPE        = "x-protobuf";            // not registered

    public static final String ALL_IMAGE_FORMATS = "image/*";
    public static final String JPEG              = "image/jpeg";
    public static final String PNG               = "image/png";
    public static final String HEIC              = "image/heic";

    public static final String GPX     = "application/gpx+xml";
    public static final String GEOJSON = "application/geo+json";

    public static final String TEXTPLAIN = "text/plain";
    public static final String TEXTXML   = "text/xml";
    public static final String TEXTCSV   = "text/comma-separated-values";

    public static final String OSMXML = "application/vnd.openstreetmap.data+xml";                   // registered
    public static final String OSMPBF = "application/vnd.openstreetmap.data+" + X_PROTOBUF_SUBTYPE; // not registered
    public static final String OSCXML = "application/vnd.openstreetmap.osc+xml";                    // not registered
    public static final String OSNXML = "application/vnd.openstreetmap.osn+xml";                    // not registered

    public static final String TODOJSON = "application/vnd.vespucci.todo+" + JSON_SUBTYPE;// not registered

    public static final String ZIP = "application/zip";

    /**
     * Private constructor
     */
    private MimeTypes() {
        // nothing
    }
}
