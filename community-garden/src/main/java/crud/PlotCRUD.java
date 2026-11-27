package crud;
import util.DatabaseFuncs;
import java.math.BigDecimal;
import java.sql.*;

public class PlotCRUD {
    public void createPlot(String plotLocation, BigDecimal plotArea, String typeCode,
                           String texCode, BigDecimal phLevel, String matCode, BigDecimal bedHeight) {
        if (plotArea == null || plotArea.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("ERROR: Usable area must be greater than zero.");
            return;
        }
        String plotStatement = """
                                INSERT INTO PLOTS (plot_code, plot_location, plot_area, type_code, rev_no) 
                                VALUES (?, ?, ?, ?, 1)
                                """;
        String groundStatement = """
                                INSERT INTO GROUND_PLOTS (plot_code, tex_code, ph_level, rev_no) 
                                VALUES (?, ?, ?, 1)
                                """;
        String raisedStatement = """
                                INSERT INTO RAISED_BEDS (plot_code, mat_code, bed_height, rev_no) 
                                VALUES (?, ?, ?, 1)
                                """;
        Connection connection = null;
        try {
            connection = DatabaseFuncs.getConnection();
            connection.setAutoCommit(false);
            String plotCode = DatabaseFuncs.generateNextCode(connection, "PLOTS", "plot_code", "P");
            try (PreparedStatement prepState = connection.prepareStatement(plotStatement)) {
                prepState.setString(1, plotCode);
                prepState.setString(2, plotLocation);
                prepState.setBigDecimal(3, plotArea);
                prepState.setString(4, typeCode);
                prepState.executeUpdate();
            }
            if ("GND".equalsIgnoreCase(typeCode)) {
                try (PreparedStatement psGround = connection.prepareStatement(groundStatement)) {
                    psGround.setString(1, plotCode);
                    psGround.setString(2, texCode);
                    psGround.setObject(3, phLevel, Types.DECIMAL);
                    psGround.executeUpdate();
                }
            } else if ("RSD".equalsIgnoreCase(typeCode)) {
                if (bedHeight == null || bedHeight.compareTo(BigDecimal.ZERO) <= 0) {
                    connection.rollback();
                    System.out.println("ERROR: Raised bed height must be greater than zero.");
                    return;
                }
                try (PreparedStatement psRaised = connection.prepareStatement(raisedStatement)) {
                    psRaised.setString(1, plotCode);
                    psRaised.setString(2, matCode);
                    psRaised.setBigDecimal(3, bedHeight);
                    psRaised.executeUpdate();
                }
            } else {
                connection.rollback();
                System.out.println("ERROR: Invalid Type Code!");
                return;
            }
            connection.commit();
            System.out.println("Plot " + plotCode + " created successfully!");
        } catch (SQLException e) {
            if (connection != null){
                try {
                    connection.rollback();
                } catch (SQLException ee) {
                    System.out.println("ERROR: Unable to revert to last snapshot due to: " + ee.getMessage());
                }
            }
            System.out.println("ERROR: Unable to create plot due to: " + e.getMessage());
        } finally {
            if (connection != null){
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    System.out.println("ERROR: Unable to close connection due to: " + e.getMessage());
                }
            }
        }
    }

    public boolean viewPlots(String plotCode, String plotLocation, String plotType){
        boolean isEmpty = true;
        String plot = (plotCode == null || plotCode.isBlank()) ? null : plotCode.trim();
        String location = (plotLocation == null || plotLocation.isBlank()) ? null : plotLocation.trim();
        String type = (plotType == null || plotType.isBlank() || plotType.equalsIgnoreCase("All")) ? null : plotType.trim();
        String plotHeader = (plot == null) ? "All" : plotCode.trim();
        String locationHeader = (location == null) ? "All" : plotLocation.trim();
        String typeHeader = (type == null) ? "All" : plotType.trim();
        String statement = """
                            SELECT  p.plot_code, p.plot_location, t.plot_type, plot_area
                            FROM    PLOTS p JOIN TYPES_LOOKUP t 
                                            ON p.type_code = t.type_code
                            WHERE (? IS NULL OR p.plot_code = UPPER(?))
                            AND (? IS NULL OR LOWER(p.plot_location) = LOWER(?))
                            AND (? IS NULL OR LOWER(t.plot_type) = LOWER(?))
                            ORDER BY   p.plot_code ASC
                            """;
        try(Connection connection = DatabaseFuncs.getConnection();
            PreparedStatement prepState = connection.prepareStatement(statement)){
            prepState.setString(1, plot);
            prepState.setString(2, plot);
            prepState.setString(3, location);
            prepState.setString(4, location);
            prepState.setString(5, type);
            prepState.setString(6, type);
            try (ResultSet result = prepState.executeQuery()){
                System.out.println("\n===========================================================");
                System.out.println("    PLOT LISTINGS");
                System.out.println("    Selected Filters: Plot: " + plotHeader);
                System.out.println("                      Location: " + locationHeader);
                System.out.println("                      Type: " + typeHeader);
                System.out.println("===========================================================");
                while (result.next()){
                    if(isEmpty){
                        System.out.printf(" %-8s | %-30s | %-8s | %-15s%n", "Plot Code", "Location", "Plot Type", "Area (sq.m)");
                        System.out.println("-----------------------------------------------------------");
                        isEmpty = false;
                    }
                    BigDecimal area = result.getBigDecimal("plot_area");
                    String areaFormat = area.toPlainString();
                    System.out.printf(" %-9s | %-30s | %-8s | %-15s%n", 
                                        result.getString("plot_code"), result.getString("plot_location"), 
                                        result.getString("plot_type"), areaFormat);
                }
                if(isEmpty){
                    System.out.println("    No plots matched the selected filters.");
                }
                System.out.println("===========================================================");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to display plots due to: " + e.getMessage());
        }
        return isEmpty;
    }

    public int loadPlot(String plotCode) {
        if (plotCode == null || plotCode.isBlank()) {
            System.out.println("ERROR: Plot code cannot be blank.");
            return -1;
        }
        String code = plotCode.trim().toUpperCase();
        String plotStatement = """
                                SELECT  p.plot_code, p.plot_location, p.type_code, t.plot_type,
                                        p.plot_area, p.rev_no, tx.soil_tex, gp.ph_level, 
                                        m.bed_mat, rb.bed_height 
                                FROM PLOTS p    JOIN TYPES_LOOKUP t             ON p.type_code = t.type_code
                                                LEFT JOIN GROUND_PLOTS gp       ON p.plot_code = gp.plot_code
                                                LEFT JOIN TEXTURES tx           ON gp.tex_code = tx.tex_code
                                                LEFT JOIN RAISED_BEDS rb        ON p.plot_code = rb.plot_code
                                                LEFT JOIN MATERIALS m           ON rb.mat_code = m.mat_code
                                WHERE p.plot_code = UPPER(?)
                                """;

        String assignmentStatement = """
                                    SELECT a.assign_code, a.hh_code, h.hh_label, a.start_date, a.end_date, a.remarks, a.rev_no
                                    FROM ASSIGNMENTS a JOIN HOUSEHOLDS h ON a.hh_code = h.hh_code
                                    WHERE a.plot_code = UPPER(?)
                                    ORDER BY a.start_date DESC, a.assign_code ASC
                                    """;

        String repairStatement = """
                                    SELECT repair_code, repair_date, work_desc, cost, rev_no
                                    FROM BED_REPAIRS
                                    WHERE plot_code = UPPER(?)
                                    ORDER BY repair_date DESC, repair_code ASC
                                    """;
        try (Connection connection = DatabaseFuncs.getConnection()) {
            String typeCode;
            int loadedRevNo;
            try (PreparedStatement prepState = connection.prepareStatement(plotStatement)) {
                prepState.setString(1, code);
                try (ResultSet result = prepState.executeQuery()) {
                    if (!result.next()) {
                        System.out.println("Plot '" + code + "' does not exist.");
                        return -1;
                    }
                    typeCode = result.getString("type_code");
                    loadedRevNo = result.getInt("rev_no");
                    System.out.println("\n===========================================================");
                    System.out.println("    SELECTED PLOT: " + result.getString("plot_code"));
                    System.out.println("    Location : " + result.getString("plot_location"));
                    System.out.println("    Area     : " + result.getBigDecimal("plot_area") + " sq.m");
                    System.out.println("    Type     : " + result.getString("plot_type"));
                    if ("GND".equalsIgnoreCase(typeCode)) {
                        BigDecimal ph = result.getBigDecimal("ph_level");
                        System.out.println("    Soil Tex : " + result.getString("soil_tex"));
                        System.out.println("    Soil pH  : " + (ph == null ? "Unassessed" : ph.toPlainString()));
                    } else {
                        System.out.println("    Material : " + result.getString("bed_mat"));
                        System.out.println("    Height   : " + result.getBigDecimal("bed_height") + " cm");
                    }
                }
            }
            System.out.println("===========================================================");
            System.out.println("    ASSIGNMENT HISTORY");
            try (PreparedStatement prepState = connection.prepareStatement(assignmentStatement)) {
                prepState.setString(1, code);
                try (ResultSet result = prepState.executeQuery()) {
                    boolean hasAssign = false;
                    while (result.next()) {
                        hasAssign = true;
                        Date end = result.getDate("end_date");
                        String remarks = result.getString("remarks");
                        System.out.printf("    [%s] %s (%s) | %s to %s | Remarks: %s%n",
                                result.getString("assign_code"),
                                result.getString("hh_label"),
                                result.getString("hh_code"),
                                result.getDate("start_date"),
                                (end == null) ? "Ongoing" : end.toString(),
                                (remarks == null || remarks.isBlank()) ? "None" : remarks);
                    }
                    if (!hasAssign) {
                        System.out.println("    No assignments recorded for this plot.");
                    }
                }
            }
            if ("RSD".equalsIgnoreCase(typeCode)) {
                System.out.println("===========================================================");
                System.out.println("    RAISED-BED STRUCTURAL REPAIRS");
                try (PreparedStatement prepState = connection.prepareStatement(repairStatement)) {
                    prepState.setString(1, code);
                    try (ResultSet result = prepState.executeQuery()) {
                        boolean hasRepairs = false;
                        while (result.next()) {
                            hasRepairs = true;
                            BigDecimal cost = result.getBigDecimal("cost");
                            String costDisplay = (cost == null) ? "" : "PHP " + cost.toPlainString();
                            System.out.printf("    [%s] %s | Cost: %-10s | %s%n",
                                    result.getString("repair_code"),
                                    result.getDate("repair_date"),
                                    costDisplay,
                                    result.getString("work_desc"));
                        }
                        if (!hasRepairs) {
                            System.out.println("    No repairs recorded for this raised bed.");
                        }
                    }
                }
            }
            System.out.println("===========================================================");
            return loadedRevNo;
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to load plot due to: " + e.getMessage());
            return -1;
        }
    }

    public String getPlotTypeCode(String plotCode) {
        if (plotCode == null || plotCode.isBlank()) {
            return null;
        }
        String statement = """
                            SELECT type_code 
                            FROM PLOTS 
                            WHERE UPPER(plot_code) = ?
                            """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, plotCode.trim().toUpperCase());
            try (ResultSet result = prepState.executeQuery()) {
                if (result.next()) {
                    return result.getString("type_code");
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to check plot type due to: " + e.getMessage());
        }
        return null;
    }

    public void updatePlot(String plotCode, String newLocation, BigDecimal newArea,
                           String newTexCode, BigDecimal newPh, String newMatCode, 
                           BigDecimal newHeight, int loadedRevNo) {
        if (newArea == null || newArea.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("ERROR: Usable area must be greater than zero.");
            return;
        }
        String code = plotCode.trim().toUpperCase();
        String checkPlantArea = """
                                SELECT COALESCE(SUM(pl.plant_area), 0) AS active_area
                                FROM PLANTS pl  JOIN ASSIGNMENTS a  ON pl.assign_code = a.assign_code
                                WHERE a.plot_code = UPPER(?) 
                                AND pl.close_date IS NULL
                                """;
        String checkType = """
                            SELECT type_code 
                            FROM PLOTS 
                            WHERE plot_code = UPPER(?)
                            """;
        String updateStatement = """
                                UPDATE PLOTS
                                SET plot_location = ?, plot_area = ?, rev_no = rev_no + 1
                                WHERE plot_code = UPPER(?) AND rev_no = ?
                                """;
        String updateGround = """
                                UPDATE GROUND_PLOTS 
                                SET tex_code = ?, ph_level = ?, rev_no = rev_no + 1 
                                WHERE plot_code = UPPER(?)
                                """;
        String updateRaised = """
                                UPDATE RAISED_BEDS 
                                SET mat_code = ?, bed_height = ?, rev_no = rev_no + 1 
                                WHERE plot_code = UPPER(?)
                                """;
        Connection connection = null;
        try {
            connection = DatabaseFuncs.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement checker = connection.prepareStatement(checkPlantArea)) {
                checker.setString(1, code);
                try (ResultSet result = checker.executeQuery()) {
                    if (result.next()) {
                        BigDecimal activeArea = result.getBigDecimal("active_area");
                        if (newArea.compareTo(activeArea) < 0) {
                            connection.rollback();
                            System.out.println("ERROR: New area (" + newArea + " sq.m) is smaller than currently planted area (" + activeArea + " sq.m).");
                            return;
                        }
                    }
                }
            }
            String typeCode = null;
            try (PreparedStatement typecheck = connection.prepareStatement(checkType)) {
                typecheck.setString(1, code);
                try (ResultSet result = typecheck.executeQuery()) {
                    if (result.next()) {
                        typeCode = result.getString("type_code");
                    }
                }
            }
            if (typeCode == null || (!typeCode.equalsIgnoreCase("GND") && !typeCode.equalsIgnoreCase("RSD"))) {
                connection.rollback();
                System.out.println("ERROR: Plot type '" + typeCode + "' does not exist.");
                return;
            }
            try (PreparedStatement prepState = connection.prepareStatement(updateStatement)) {
                prepState.setString(1, newLocation.trim());
                prepState.setBigDecimal(2, newArea);
                prepState.setString(3, code);
                prepState.setInt(4, loadedRevNo);
                if (prepState.executeUpdate() == 0) {
                    connection.rollback();
                    System.out.println("ERROR: Stale revision number. Please reload the plot and try again.");
                    return;
                }
            }
            if ("GND".equalsIgnoreCase(typeCode)) {
                try (PreparedStatement updateState = connection.prepareStatement(updateGround)) {
                    updateState.setString(1, newTexCode.trim().toUpperCase());
                    updateState.setObject(2, newPh, Types.DECIMAL);
                    updateState.setString(3, code);
                    updateState.executeUpdate();
                }
            } else if ("RSD".equalsIgnoreCase(typeCode)) {
                if (newHeight == null || newHeight.compareTo(BigDecimal.ZERO) <= 0) {
                    connection.rollback();
                    System.out.println("Error: Raised bed height must be greater than zero.");
                    return;
                }
                try (PreparedStatement updateState = connection.prepareStatement(updateRaised)) {
                    updateState.setString(1, newMatCode.trim().toUpperCase());
                    updateState.setBigDecimal(2, newHeight);
                    updateState.setString(3, code);
                    updateState.executeUpdate();
                }
            }
            connection.commit();
            System.out.println("Plot '" + code + "' updated successfully!");
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback(); 
                } catch (SQLException ee) {
                    System.out.println("ERROR: Unable to revert to last snapshot due to: " + ee.getMessage());
                }
            }
            System.out.println("ERROR: Unable to update plot due to: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    System.out.println("ERROR: Unable to close connection due to: " + e.getMessage());
                }
            }
        }
    }

    public void deletePlot(String plotCode) {
        if (plotCode == null || plotCode.isBlank()) {
            System.out.println("ERROR: Plot code cannot be blank.");
            return;
        }
        String code = plotCode.trim().toUpperCase();
        String checkDependency = """
                                   SELECT (SELECT COUNT(*) FROM ASSIGNMENTS WHERE plot_code = UPPER(?)) +
                                          (SELECT COUNT(*) FROM BED_REPAIRS WHERE plot_code = UPPER(?)) AS total_dep
                                   """;
        String deleteGround = "DELETE FROM GROUND_PLOTS WHERE plot_code = UPPER(?)";
        String deleteRaised = "DELETE FROM RAISED_BEDS WHERE plot_code = UPPER(?)";
        String deletePlot   = "DELETE FROM PLOTS WHERE plot_code = UPPER(?)";
        Connection connection = null;
        try {
            connection = DatabaseFuncs.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement checker = connection.prepareStatement(checkDependency)) {
                checker.setString(1, code);
                checker.setString(2, code);
                try (ResultSet result = checker.executeQuery()) {
                    if (result.next() && result.getInt("total_dep") > 0) {
                        connection.rollback();
                        System.out.println("ERROR: Delete blocked. Plot '" + code + "' has existing assignments or repairs.");
                        return;
                    }
                }
            }
            try (PreparedStatement deleteG = connection.prepareStatement(deleteGround);
                 PreparedStatement deleteR = connection.prepareStatement(deleteRaised);
                 PreparedStatement deleteP = connection.prepareStatement(deletePlot)) {
                deleteG.setString(1, code);
                deleteG.executeUpdate();
                deleteR.setString(1, code);
                deleteR.executeUpdate();
                deleteP.setString(1, code);
                if (deleteP.executeUpdate() > 0) {
                    connection.commit();
                    System.out.println("Plot '" + code + "' deleted successfully!");
                } else {
                    connection.rollback();
                    System.out.println("ERROR: Plot '" + code + "' does not exist.");
                }
            }
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ee) {
                    System.out.println("ERROR: Unable to revert to last snapshot due to: " + ee.getMessage());
                }
            }
            System.out.println("ERROR: Unable to delete plot due to: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    System.out.println("ERROR: Unable to close connection due to: " + e.getMessage());
                }
            }
        }
    }
}