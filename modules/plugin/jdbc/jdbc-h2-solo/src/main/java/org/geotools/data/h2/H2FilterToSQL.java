/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.h2;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.filter.FilterCapabilities;
import org.h2.value.ValueGeometry;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.BinarySpatialOperator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 
 *
 * @source $URL$
 */
public class H2FilterToSQL extends FilterToSQL {

    @Override
    protected FilterCapabilities createFilterCapabilities() {
        FilterCapabilities caps = super.createFilterCapabilities();
        caps.addType(BBOX.class);
        return caps;
    }
    
    @Override
    protected void visitLiteralGeometry(Literal expression) throws IOException {
        Geometry g = (Geometry) evaluateLiteral(expression, Geometry.class);
        out.write( "'"+ValueGeometry.getFromGeometry(g).getSQL()+"'");
    }
    
    
    @Override
    protected Object visitBinarySpatialOperator(BinarySpatialOperator filter,
        PropertyName property, Literal geometry, boolean swapped, Object extraData) {
        return visitBinarySpatialOperator(filter, (Expression) property, (Expression) geometry, 
            swapped, extraData);
    }
    
    @Override
    protected Object visitBinarySpatialOperator(BinarySpatialOperator filter, Expression e1,
            Expression e2, Object extraData) {
        return visitBinarySpatialOperator(filter, e1, e2, false, extraData);
    }
    
    protected Object visitBinarySpatialOperator(BinarySpatialOperator filter, Expression e1,
                Expression e2, boolean swapped, Object extraData) {

        try {
            if (filter instanceof BBOX) {
                e1.accept(this, extraData);
                out.write(" && ");
                e2.accept(this, extraData);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return extraData;
    }

    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static{
        // Set DATE_FORMAT time zone to GMT, as Date's are always in GMT internaly. Otherwise we'll
        // get a local timezone encoding regardless of the actual Date value        
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
    
    @Override
    protected void writeLiteral(Object literal) throws IOException {
        if (literal instanceof Date) {
            out.write("PARSEDATETIME(");
            if (literal instanceof java.sql.Date) {
                out.write("'" + DATE_FORMAT.format(literal) + "', 'yyyy-MM-dd'");
            }
            else {
                out.write("'" + DATETIME_FORMAT.format(literal) + "', 'yyyy-MM-dd HH:mm:ss.SSSZ'");
            }
            out.write(")");
        }
        else {
            super.writeLiteral(literal);
        }
    }
}
