package crud;
import util.DatabaseFuncs;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class BedRepairCRUD {
    public void createRepair(String plotCode, LocalDate repDate, String workDesc, BigDecimal cost) {
        if (plotCode == null || plotCode.isBlank()) {
            System.out.println("ERROR: Plot code is required.");
            return;
        }
        if (repDate == null) {
            System.out.println("ERROR: Repair date is required.");
            return;
        }
        if (repDate.isAfter(LocalDate.now())) {
            System.out.println("ERROR: Repair date cannot be in the future.");
            return;
        }
        if (workDesc == null || workDesc.isBlank()) {
            System.out.println("ERROR: Completed work description cannot be blank.");
            return;
        }
        if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("ERROR: Repair expense cannot be negative.");
            return;
        }
        String code = plotCode.trim().toUpperCase();
        String desc = workDesc.trim();
        String checkStatement = """
                                SELECT 1
                                FROM RAISED_BEDS 
                                WHERE plot_code = UPPER(?)
                                """;
        String statement = """
                                 INSERT INTO BED_REPAIRS (repair_code, plot_code, repair_date, work_desc, cost, rev_no)
                                 VALUES (?, ?, ?, ?, ?, 1)
                                 """;
        Connection connection = null;
        try {
            connection = DatabaseFuncs.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement checker = connection.prepareStatement(checkStatement)) {
                checker.setString(1, code);
                try (ResultSet result = checker.executeQuery()) {
                    if (!result.next()) {
                        connection.rollback();
                        System.out.println("ERROR: Plot '" + code + "' is not a registered raised bed.");
                        return;
                    }
                }
            }
            String repairCode = DatabaseFuncs.generateNextCode(connection, "BED_REPAIRS", "repair_code", "R");
            try (PreparedStatement prepState = connection.prepareStatement(statement)) {
                prepState.setString(1, repairCode);
                prepState.setString(2, code);
                prepState.setDate(3, Date.valueOf(repDate));
                prepState.setString(4, desc);
                prepState.setObject(5, cost, Types.DECIMAL);
                prepState.executeUpdate();
            }
            connection.commit();
            System.out.println("Bed repair record '" + repairCode + "' recorded successfully!");
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ee) {
                    System.out.println("ERROR: Unable to revert to last snapshot due to: " + ee.getMessage());
                }
            }
            System.out.println("ERROR: Unable to record repair due to: " + e.getMessage());
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

    public boolean viewRepairs(String repairFilter, String plotFilter, LocalDate dateFrom, LocalDate dateTo) {
        boolean isEmpty = true;
        String repairCode = (repairFilter == null || repairFilter.isBlank()) ? null : repairFilter.trim().toUpperCase();
        String plotCode = (plotFilter == null || plotFilter.isBlank()) ? null : plotFilter.trim().toUpperCase();
        String repairHeader = (repairCode == null) ? "All" : repairCode;
        String plotHeader = (plotCode == null) ? "All" : plotCode;
        String rangeHeader = (dateFrom == null && dateTo == null) ? "All Dates"
                           : (dateFrom == null ? "Any" : dateFrom) + " to " + (dateTo == null ? "Any" : dateTo);
        String statement = """
                            SELECT repair_code, plot_code, repair_date, work_desc, cost
                            FROM BED_REPAIRS
                            WHERE (? IS NULL OR repair_code = UPPER(?))
                            AND (? IS NULL OR plot_code = UPPER(?))
                            AND (? IS NULL OR repair_date >= ?)
                            AND (? IS NULL OR repair_date <= ?)
                            ORDER BY repair_date DESC, repair_code ASC
                            """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, repairCode);
            prepState.setString(2, repairCode);
            prepState.setString(3, plotCode);
            prepState.setString(4, plotCode);
            prepState.setObject(5, (dateFrom != null) ? Date.valueOf(dateFrom) : null, Types.DATE);
            prepState.setObject(6, (dateFrom != null) ? Date.valueOf(dateFrom) : null, Types.DATE);
            prepState.setObject(7, (dateTo != null) ? Date.valueOf(dateTo) : null, Types.DATE);
            prepState.setObject(8, (dateTo != null) ? Date.valueOf(dateTo) : null, Types.DATE);
            try (ResultSet result = prepState.executeQuery()) {
                System.out.println("\n===============================================================================");
                System.out.println("    RAISED-BED REPAIR LISTINGS");
                System.out.println("    Selected Filters: Repair: " + repairHeader);
                System.out.println("                      Plot: " + plotHeader);
                System.out.println("                      Repair Range: " + rangeHeader);
                System.out.println("===============================================================================");
                while (result.next()) {
                    if (isEmpty) {
                        System.out.printf(" %-8s | %-6s | %-12s | %-12s | %-30s%n",
                                "Repair", "Plot", "Date", "Cost (PHP)", "Description");
                        System.out.println("-------------------------------------------------------------------------------");
                        isEmpty = false;
                    }
                    BigDecimal cost = result.getBigDecimal("cost");
                    String costDisplay = (cost == null) ? "" : cost.toPlainString();
                    System.out.printf(" %-8s | %-6s | %-12s | %-12s | %-30s%n",
                            result.getString("repair_code"),
                            result.getString("plot_code"),
                            result.getDate("repair_date"),
                            costDisplay,
                            result.getString("work_desc"));
                }
                if (isEmpty) {
                    System.out.println("    No repairs recorded matching the selected filters.");
                }
                System.out.println("===============================================================================");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to display repairs due to: " + e.getMessage());
        }
        return isEmpty;
    }

    public int loadRepair(String repairCode) {
        if (repairCode == null || repairCode.isBlank()) {
            System.out.println("ERROR: Repair code cannot be blank.");
            return -1;
        }
        String code = repairCode.trim().toUpperCase();
        String statement = """
                           SELECT repair_code, plot_code, repair_date, work_desc, cost, rev_no
                           FROM BED_REPAIRS
                           WHERE repair_code = UPPER(?)
                           """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, code);
            try (ResultSet result = prepState.executeQuery()) {
                if (!result.next()) {
                    System.out.println("Repair '" + code + "' does not exist.");
                    return -1;
                }
                BigDecimal cost = result.getBigDecimal("cost");
                String costDisplay = (cost == null) ? "(Unknown)" : "PHP " + cost.toPlainString();
                int loadedRevNo = result.getInt("rev_no");
                System.out.println("\n============================================================");
                System.out.println("    LOADED REPAIR: " + result.getString("repair_code"));
                System.out.println("    Plot        : " + result.getString("plot_code"));
                System.out.println("    Repair Date : " + result.getDate("repair_date"));
                System.out.println("    Expense     : " + costDisplay);
                System.out.println("    Description : " + result.getString("work_desc"));
                System.out.println("============================================================");
                return loadedRevNo;
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to load repair due to: " + e.getMessage());
            return -1;
        }
    }

    public void updateRepair(String repairCode, LocalDate newDate, String newDesc,
                             BigDecimal newCost, int loadedRevNo) {
        if (repairCode == null || repairCode.isBlank()) {
            System.out.println("ERROR: Repair code is required.");
            return;
        }
        if (newDate == null) {
            System.out.println("ERROR: Repair date is required.");
            return;
        }
        if (newDate.isAfter(LocalDate.now())) {
            System.out.println("ERROR: Repair date cannot be in the future.");
            return;
        }
        if (newDesc == null || newDesc.isBlank()) {
            System.out.println("ERROR: Completed work description cannot be blank.");
            return;
        }
        if (newCost != null && newCost.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("ERROR: Repair expense cannot be negative.");
            return;
        }
        String code = repairCode.trim().toUpperCase();
        String desc = newDesc.trim();
        String statement = """
                            UPDATE BED_REPAIRS
                            SET repair_date = ?, work_desc = ?, cost = ?, rev_no = rev_no + 1
                            WHERE repair_code = UPPER(?) AND rev_no = ?
                            """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setDate(1, Date.valueOf(newDate));
            prepState.setString(2, desc);
            prepState.setObject(3, newCost, Types.DECIMAL);
            prepState.setString(4, code);
            prepState.setInt(5, loadedRevNo);
            if (prepState.executeUpdate() > 0) {
                System.out.println("Repair '" + code + "' updated successfully!");
            } else {
                System.out.println("ERROR: Update rejected. Repair not found or stale revision number.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to update repair due to: " + e.getMessage());
        }
    }

    public void deleteRepair(String repairCode) {
        if (repairCode == null || repairCode.isBlank()) {
            System.out.println("ERROR: Repair code cannot be blank.");
            return;
        }
        String code = repairCode.trim().toUpperCase();
        String statement = "DELETE FROM BED_REPAIRS WHERE repair_code = UPPER(?)";
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, code);
            if (prepState.executeUpdate() > 0) {
                System.out.println("Repair '" + code + "' deleted successfully!");
            } else {
                System.out.println("ERROR: Repair '" + code + "' does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to delete repair due to: " + e.getMessage());
        }
    }
}
