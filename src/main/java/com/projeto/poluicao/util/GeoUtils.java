package com.projeto.poluicao.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GeoUtils {

    // SRID 4326 é o padrão mundial WGS84 para Latitude e Longitude
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static Point criarPonto(double latitude, double longitude) {
        // No JTS/PostGIS a ordem da coordenada é (Longitude, Latitude)
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}