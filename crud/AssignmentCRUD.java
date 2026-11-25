package crud;
import util.DatabaseFuncs;
import java.sql.*;
import java.time.LocalDate;

public class AssignmentCRUD {
    public void createAssignment(String hhCode, String plotCode, LocalDate startDate, 
                                 LocalDate endDate, String remarks) {
        if (hhCode == null || hhCode.isBlank()) {
            System.out.println("ERROR: Household code is required.");
            return;
        }
        if (plotCode == null || plotCode.isBlank()) {
            System.out.println("ERROR: Plot code is required.");
            return;
        }
        if (startDate == null) {
            System.out.println("ERROR: Start date is required.");
            return;
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            System.out.println("ERROR: End date cannot precede start date.");
            return;
        }
        String household = hhCode.trim().toUpperCase();
        String plot = plotCode.trim().toUpperCase();
        String remarkChecked = (remarks == null || remarks.isBlank()) ? null : remarks.trim();
        String checkDateValidity = """
                                SELECT COUNT(*) AS date_overlap
                                FROM ASSIGNMENTS
                                WHERE plot_code = UPPER(?)
                                AND start_date <= COALESCE(?, '9999-12-31')
                                AND COALESCE(end_date, '9999-12-31') >= ?
                              """;
        String statement = """
                                 INSERT INTO ASSIGNMENTS (assign_code, hh_code, plot_code, start_date, end_date, remarks, rev_no)
                                 VALUES (?, ?, ?, ?, ?, ?, 1)
                                 """;
        Connection connection = null;
        try {
            connection = DatabaseFuncs.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement checker = connection.prepareStatement(checkDateValidity)) {
                checker.setString(1, plot);
                checker.setObject(2, (endDate != null) ? Date.valueOf(endDate) : null, Types.DATE);
                checker.setDate(3, Date.valueOf(startDate));
                try (ResultSet result = checker.executeQuery()) {
                    if (result.next() && result.getInt("date_overlap") > 0) {
                        connection.rollback();
                        System.out.println("ERROR: Plot '" + plot + "' already has an overlapping assignment during those dates.");
                        return;
                    }
                }
            }
            String assignCode = DatabaseFuncs.generateNextCode(connection, "ASSIGNMENTS", "assign_code", "A");
            try (PreparedStatement prepState = connection.prepareStatement(statement)) {
                prepState.setString(1, assignCode);
                prepState.setString(2, household);
                prepState.setString(3, plot);
                prepState.setDate(4, Date.valueOf(startDate));
                prepState.setObject(5, (endDate != null) ? Date.valueOf(endDate) : null, Types.DATE);
                prepState.setString(6, remarkChecked);
                prepState.executeUpdate();
            }
            connection.commit();
            System.out.println("Assignment '" + assignCode + "' created successfully!");
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ee) {
                    System.out.println("ERROR: Unable to revert to last snapshot due to: " + ee.getMessage());
                }
            }
            System.out.println("ERROR: Unable to create assignment due to: " + e.getMessage());
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

    public boolean viewAssignments(String assignFilter, String hhFilter, String plotFilter,
                                   LocalDate startFrom, LocalDate startTo) {
        boolean isEmpty = true;
        String assignCode = (assignFilter == null || assignFilter.isBlank()) ? null : assignFilter.trim().toUpperCase();
        String householdLabel = (hhFilter == null || hhFilter.isBlank()) ? null : hhFilter.trim();
        String plotCode = (plotFilter == null || plotFilter.isBlank()) ? null : plotFilter.trim().toUpperCase();
        String assignHeader = (assignCode == null) ? "All" : assignCode;
        String householdHeader = (householdLabel == null) ? "All" : householdLabel;
        String plotHeader = (plotCode == null) ? "All" : plotCode;
        String rangeHeader = (startFrom == null && startTo == null) ? "All Dates"
                           : (startFrom == null ? "Any" : startFrom) + " to " + (startTo == null ? "Any" : startTo);
        String statement = """
                            SELECT a.assign_code, a.hh_code, h.hh_label, a.plot_code, a.start_date, a.end_date
                            FROM ASSIGNMENTS a
                            JOIN HOUSEHOLDS h ON a.hh_code = h.hh_code
                            WHERE (? IS NULL OR a.assign_code = UPPER(?))
                            AND (? IS NULL OR LOWER(h.hh_label) LIKE LOWER(CONCAT('%', ?, '%')))
                            AND (? IS NULL OR a.plot_code = UPPER(?))
                            AND (? IS NULL OR a.start_date >= ?)
                            AND (? IS NULL OR a.start_date <= ?)
                            ORDER BY a.start_date DESC, a.assign_code ASC
                            """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, assignCode);
            prepState.setString(2, assignCode);
            prepState.setString(3, householdLabel);
            prepState.setString(4, householdLabel);
            prepState.setString(5, plotCode);
            prepState.setString(6, plotCode);
            prepState.setObject(7, (startFrom != null) ? Date.valueOf(startFrom) : null, Types.DATE);
            prepState.setObject(8, (startFrom != null) ? Date.valueOf(startFrom) : null, Types.DATE);
            prepState.setObject(9, (startTo != null) ? Date.valueOf(startTo) : null, Types.DATE);
            prepState.setObject(10, (startTo != null) ? Date.valueOf(startTo) : null, Types.DATE);
            try (ResultSet result = prepState.executeQuery()) {
                System.out.println("\n===============================================================================");
                System.out.println("    SUPPORTING ASSIGNMENT LISTINGS");
                System.out.println("    Selected Filters: Assignment Code: " + assignHeader);
                System.out.println("                      Household: " + householdHeader);
                System.out.println("                      Plot: " + plotHeader);
                System.out.println("                      Range: " + rangeHeader);
                System.out.println("===============================================================================");
                while (result.next()) {
                    if (isEmpty) {
                        System.out.printf(" %-8s | %-30s | %-6s | %-12s | %-12s%n",
                                "Assign", "Household", "Plot", "Start Date", "End Date");
                        System.out.println("-------------------------------------------------------------------------------");
                        isEmpty = false;
                    }
                    Date end = result.getDate("end_date");
                    String householdDisplay = result.getString("hh_label") + " (" + result.getString("hh_code") + ")";
                    System.out.printf(" %-8s | %-24s | %-6s | %-12s | %-12s%n",
                                        result.getString("assign_code"),
                                        householdDisplay,
                                        result.getString("plot_code"),
                                        result.getDate("start_date"),
                                        (end == null) ? "Ongoing" : end.toString());
                }
                if (isEmpty) {
                    System.out.println("    No assignments matched the selected filters.");
                }
                System.out.println("===============================================================================");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to display assignments due to: " + e.getMessage());
        }
        return isEmpty;
    }

    public int loadAssignment(String assignCode) {
        if (assignCode == null || assignCode.isBlank()) {
            System.out.println("ERROR: Assignment code cannot be blank.");
            return -1;
        }
        String code = assignCode.trim().toUpperCase();
        String statement = """
                            SELECT  a.assign_code, a.hh_code, h.hh_label, a.plot_code,
                                    a.start_date, a.end_date, a.remarks, a.rev_no
                            FROM ASSIGNMENTS a   JOIN HOUSEHOLDS h ON a.hh_code = h.hh_code
                            WHERE a.assign_code = UPPER(?)
                           """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, code);
            try (ResultSet result = prepState.executeQuery()) {
                if (!result.next()) {
                    System.out.println("Assignment '" + code + "' does not exist.");
                    return -1;
                }
                Date end = result.getDate("end_date");
                String remarks = result.getString("remarks");
                int loadedRevNo = result.getInt("rev_no");
                System.out.println("\n===========================================================");
                System.out.println("    LOADED ASSIGNMENT: " + result.getString("assign_code"));
                System.out.println("    Household  : " + result.getString("hh_label") + " (" + result.getString("hh_code") + ")");
                System.out.println("    Plot       : " + result.getString("plot_code"));
                System.out.println("    Start Date : " + result.getDate("start_date"));
                System.out.println("    End Date   : " + (end == null ? "Ongoing" : end.toString()));
                System.out.println("    Remarks    : " + (remarks == null ? "None" : remarks));
                System.out.println("===========================================================");
                return loadedRevNo;
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to load assignment due to: " + e.getMessage());
            return -1;
        }
    }

    public LocalDate getAssignmentStartDate(String assignCode) {
        if (assignCode == null || assignCode.isBlank()) {
            return null;
        }
        String statement = "SELECT start_date FROM ASSIGNMENTS WHERE UPPER(assign_code) = ?";
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, assignCode.trim().toUpperCase());
            try (ResultSet result = prepState.executeQuery()) {
                if (result.next()) {
                    Date start = result.getDate("start_date");
                    return (start != null) ? start.toLocalDate() : null;
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to check start date: " + e.getMessage());
        }
        return null;
    }

    public void printHouseholds() {
        String statement = "SELECT hh_code, hh_label FROM HOUSEHOLDS ORDER BY hh_label ASC, hh_code ASC";
        try (Connection connection = DatabaseFuncs.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet result = stmt.executeQuery(statement)) {
            System.out.println("============================================================");
            System.out.println("    REGISTERED HOUSEHOLDS");
            System.out.println("============================================================");
            boolean hasRows = false;
            while (result.next()) {
                hasRows = true;
                System.out.printf("    [%s] %s%n",
                        result.getString("hh_code"),
                        result.getString("hh_label"));
            }
            if (!hasRows) {
                System.out.println("    No households found in the database.");
            }
            System.out.println("============================================================");
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to load households due to: " + e.getMessage());
        }
    }

    public void releaseAssignment(String assignCode, LocalDate endDate, String remarks, int loadedRevNo) {
        if (assignCode == null || assignCode.isBlank()) {
            System.out.println("ERROR: Assignment code is required.");
            return;
        }
        if (endDate == null) {
            System.out.println("ERROR: Release end date is required.");
            return;
        }
        String code = assignCode.trim().toUpperCase();
        String remarkChecked = (remarks == null || remarks.isBlank()) ? null : remarks.trim();
        String statement = """
                            UPDATE ASSIGNMENTS
                            SET end_date = ?, remarks = ?, rev_no = rev_no + 1
                            WHERE assign_code = UPPER(?)
                            AND rev_no = ?
                            AND ? > start_date
                            AND NOT EXISTS (SELECT 1
                                            FROM PLANTS
                                            WHERE assign_code = UPPER(?)
                                            AND (close_date IS NULL OR close_date > ?))
                           """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setDate(1, Date.valueOf(endDate));
            prepState.setString(2, remarkChecked);
            prepState.setString(3, code);
            prepState.setInt(4, loadedRevNo);
            prepState.setDate(5, Date.valueOf(endDate));
            prepState.setString(6, code);
            prepState.setDate(7, Date.valueOf(endDate));
            if (prepState.executeUpdate() > 0) {
                System.out.println("Assignment '" + code + "' released successfully!");
            } else {
                System.out.println("ERROR: Stale revision number, end date is less than start date, or active planting batches exist.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to release assignment due to: " + e.getMessage());
        }
    }

    public void updateAssignment(String assignCode, LocalDate newStart, LocalDate newEnd, String newRemarks, int loadedRevNo) {
        if (assignCode == null || assignCode.isBlank() || newStart == null) {
            System.out.println("ERROR: Assignment code and start date are required.");
            return;
        }
        if (newEnd != null && newEnd.isBefore(newStart)) {
            System.out.println("ERROR: End date cannot precede start date.");
            return;
        }
        String code = assignCode.trim().toUpperCase();
        String remarkChecked = (newRemarks == null || newRemarks.isBlank()) ? null : newRemarks.trim();
        String getPlot = """
                            SELECT plot_code 
                            FROM ASSIGNMENTS 
                            WHERE UPPER(assign_code) = ?
                            """;
        String checkDateValidity = """
                                    SELECT 1
                                    FROM ASSIGNMENTS
                                    WHERE plot_code = UPPER(?)
                                    AND assign_code <> UPPER(?)
                                    AND start_date <= COALESCE(?, '9999-12-31')
                                    AND COALESCE(end_date, '9999-12-31') >= ?
                                    """;
        String checkPlants = """
                            SELECT 1
                            FROM PLANTS
                            WHERE UPPER(assign_code) = ?
                            AND (plant_date < ? OR (? IS NOT NULL AND (close_date IS NULL OR close_date > ?)))
                            """;
        String updateStatement = """
                                UPDATE ASSIGNMENTS
                                SET start_date = ?, end_date = ?, remarks = ?, rev_no = rev_no + 1
                                WHERE UPPER(assign_code) = ? AND rev_no = ?
                                """;
        Connection connection = null;
        try {
            connection = DatabaseFuncs.getConnection();
            connection.setAutoCommit(false);
            String plotCode = null;
            try (PreparedStatement plotState = connection.prepareStatement(getPlot)) {
                plotState.setString(1, code);
                try (ResultSet result = plotState.executeQuery()) {
                    if (result.next()) {
                        plotCode = result.getString("plot_code");
                    }
                }
            }
            if (plotCode == null) {
                connection.rollback();
                System.out.println("ERROR: Assignment '" + code + "' does not exist.");
                return;
            }
            try (PreparedStatement checker = connection.prepareStatement(checkDateValidity)) {
                checker.setString(1, plotCode.toUpperCase());
                checker.setString(2, code);
                checker.setObject(3, (newEnd != null) ? Date.valueOf(newEnd) : null, Types.DATE);
                checker.setDate(4, Date.valueOf(newStart));
                try (ResultSet result = checker.executeQuery()) {
                    if (result.next()) {
                        connection.rollback();
                        System.out.println("ERROR: New dates overlap with another assignment on plot '" + plotCode + "'.");
                        return;
                    }
                }
            }
            try (PreparedStatement checker = connection.prepareStatement(checkPlants)) {
                checker.setString(1, code);
                checker.setDate(2, Date.valueOf(newStart));
                checker.setObject(3, (newEnd != null) ? Date.valueOf(newEnd) : null, Types.DATE);
                checker.setObject(4, (newEnd != null) ? Date.valueOf(newEnd) : null, Types.DATE);
                try (ResultSet result = checker.executeQuery()) {
                    if (result.next()) {
                        connection.rollback();
                        System.out.println("ERROR: Recorded planting batches fall outside the new assignment dates.");
                        return;
                    }
                }
            }
            try (PreparedStatement prepState = connection.prepareStatement(updateStatement)) {
                prepState.setDate(1, Date.valueOf(newStart));
                prepState.setObject(2, (newEnd != null) ? Date.valueOf(newEnd) : null, Types.DATE);
                prepState.setString(3, remarkChecked);
                prepState.setString(4, code);
                prepState.setInt(5, loadedRevNo);
                if (prepState.executeUpdate() > 0) {
                    connection.commit();
                    System.out.println("Assignment '" + code + "' updated successfully!");
                } else {
                    connection.rollback();
                    System.out.println("ERROR: Stale revision number. Please reload the assignment and try again.");
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
            System.out.println("ERROR: Unable to update assignment due to: " + e.getMessage());
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

    public void deleteAssignment(String assignCode) {
        if (assignCode == null || assignCode.isBlank()) {
            System.out.println("ERROR: Assignment code cannot be blank.");
            return;
        }
        String code = assignCode.trim().toUpperCase();
        String statement = """
                            DELETE FROM ASSIGNMENTS
                            WHERE UPPER(assign_code) = ?
                            AND NOT EXISTS (SELECT 1
                                            FROM PLANTS
                                            WHERE assign_code = UPPER(?))
                           """;
        try (Connection connection = DatabaseFuncs.getConnection();
             PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, code);
            prepState.setString(2, code);
            if (prepState.executeUpdate() > 0) {
                System.out.println("Unused assignment '" + code + "' deleted successfully!");
            } else {
                System.out.println("ERROR: Assignment '" + code + "' does not exist or already has planting records.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to delete assignment due to: " + e.getMessage());
        }
    }
}
