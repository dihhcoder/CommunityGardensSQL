package util;
import java.sql.*;
import java.time.LocalDate;

public class Report2Generation {
    public void generateReport(LocalDate startDate, LocalDate endDate, String plotCode, String plotType, int minStart) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate) || minStart < 0) {
            System.out.println("ERROR: Invalid date range or negative minimum starts.");
            return;
        }
        String plot = (plotCode == null || plotCode.isBlank()) ? null : plotCode.trim();
        String type = (plotType == null || plotType.isBlank() ||plotType.equalsIgnoreCase("All")) ? null : plotType.trim();
        String plotHeader = (plotCode == null || plotCode.isBlank()) ? "All" : plot;
        String typeHeader = (type == null) ? "All" : type;
        String statement = """
                            SELECT p.plot_code, p.plot_location, t.plot_type,
                                COUNT(a.assign_code)         AS total_assign,
                                COUNT(DISTINCT a.hh_code)    AS distinct_hh,
                                MIN(a.start_date)            AS earliest_start,
                                MAX(a.start_date)            AS latest_start
                            FROM PLOTS p    JOIN TYPES_LOOKUP t 
                                            ON p.type_code = t.type_code
                                            LEFT JOIN ASSIGNMENTS a 
                                            ON p.plot_code = a.plot_code 
                                            AND a.start_date BETWEEN ? AND ?
                            WHERE (? IS NULL OR p.plot_code = UPPER(?))
                            AND (? IS NULL OR LOWER(t.plot_type) = LOWER(?))
                            GROUP BY p.plot_code, p.plot_location, t.plot_type
                            HAVING COUNT(a.assign_code) >= ?
                            ORDER BY total_assign DESC, p.plot_code ASC
                            """;
        System.out.println("\n================================================================================================================================");
        System.out.println("    REPORT 2: PLOT ALLOCATION STARTS");
        System.out.println("    Period: " + startDate + " to " + endDate + " | Min Assignment-Start: " + minStart );
        System.out.println("    Selected Plot: " + plotHeader + " | Selected Type: " + typeHeader);
        System.out.println("================================================================================================================================");
        try (Connection connection = DatabaseFuncs.getConnection();
            PreparedStatement prepStatement = connection.prepareStatement(statement)) {
            prepStatement.setDate(1, Date.valueOf(startDate));
            prepStatement.setDate(2, Date.valueOf(endDate));
            prepStatement.setString(3, plot);
            prepStatement.setString(4, plot);
            prepStatement.setString(5, type);
            prepStatement.setString(6, type);
            prepStatement.setInt(7, minStart);
            try(ResultSet result = prepStatement.executeQuery()){
                boolean isEmpty = true;
                while (result.next()) {
                    if(isEmpty){
                        System.out.printf("%-12s | %-30s | %-12s | %-14s | %-15s | %-13s | %-13s%n",
                                        "Plot Code", "Location", "Plot Type", "Total Starts", 
                                        "Total Households", "Earliest", "Latest");
                        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
                        isEmpty = false;
                    }
                    Date earliest = result.getDate("earliest_start");
                    Date latest = result.getDate("latest_start");
                    String earlyFormat = (earliest == null) ? "" : earliest.toString();
                    String lateFormat = (latest == null) ? "" : latest.toString();
                    System.out.printf("%-12s | %-30s | %-12s | %-14d | %-16d | %-13s | %-13s%n", 
                                    result.getString("plot_code"), result.getString("plot_location"),
                                    result.getString("plot_type"), result.getInt("total_assign"), 
                                    result.getInt("distinct_hh"), earlyFormat, lateFormat);
                }
                if (isEmpty) {
                    System.out.println("    No plots matched the selected filters.");
                }
                System.out.println("================================================================================================================================\n");
            } 
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to generate report due to: " + e.getMessage());
        }
    }
}