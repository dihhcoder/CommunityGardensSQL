package ui;
import crud.*;
import util.Report2Generation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class PlotManagement {
    private static final PlotCRUD plotCRUD = new PlotCRUD();
    private static final AssignmentCRUD assignCRUD = new AssignmentCRUD();
    private static final BedRepairCRUD repairCRUD = new BedRepairCRUD();
    private static final Report2Generation report2 = new Report2Generation();

    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        plotManagementMenu(keyboard);
        keyboard.close();
    }

    public static void plotManagementMenu(Scanner keyboard) {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("  PLOT MANAGEMENT & SUPPORTING ALLOCATION WORKFLOWS");
            System.out.println("=======================================================");
            System.out.println("[1] Create New Plot");
            System.out.println("[2] Search & Select a Plot for Management");
            System.out.println("[3] Generate Plot Allocation Starts Report");
            System.out.println("[0] Exit");
            int choice = getInt("Choose an option: ", keyboard, 0, 3);
            if (choice == 0) {
                System.out.println("Back to the main menu!");
                break;
            }
            switch (choice) {
                case 1 -> optionCreatePlot(keyboard);
                case 2 -> optionPlotManagement(keyboard);
                case 3 -> optionReport(keyboard);
            }
        }
    }

    public static void optionCreatePlot(Scanner keyboard){
        int counter = 0;
        String loc = "";
        BigDecimal area = BigDecimal.ZERO;
        int typechoice = 0;
        String typeheader = "";
        int texturechoice = 0;
        String textureheader = "";
        int materialchoice = 0;
        String materialheader = "";
        BigDecimal ph = null;
        BigDecimal height = BigDecimal.ZERO;
        while(counter < 5){
            if(counter >= 0) {
                System.out.println("\n=======================================================");
                System.out.println("                CREATE A BRAND NEW PLOT");
                System.out.println("=======================================================");
                if(counter >= 1){
                    System.out.println("    Location: " + loc);
                    if(counter >= 2){
                        System.out.println("    Area: " + area);
                        if(counter >= 3){
                            System.out.println("    Plot Type: " + typeheader);
                            if(counter >= 4){
                                if(typeheader.equals("Ground")){
                                    System.out.println("    Soil Texture: " + textureheader);
                                } else {
                                    System.out.println("    Bed Material: " + materialheader);
                                }   
                            }
                        }
                    }
                }
                if(counter != 0){
                    System.out.println("=======================================================");
                }
                switch (counter) {
                    case 0:
                        loc = getString("Plot Location: ", keyboard, true);
                        break;
                    case 1: 
                        area = getBigDecimal("Usable area (sq. m): ", keyboard, true, new BigDecimal("0.01"));
                        break;
                    case 2:
                        System.out.println("Available Plot Types: ");
                        System.out.println("[1] Ground");
                        System.out.println("[2] Raised");
                        typechoice = getInt("Choose plot type: ", keyboard, 1, 2);
                        typeheader = (typechoice == 1) ? "Ground" : "Raised";
                        break;
                    case 3:
                        if(typechoice == 1){
                            System.out.println("Available Soil Textures: ");
                            System.out.println("[1] Sandy");
                            System.out.println("[2] Loamy");
                            System.out.println("[3] Clayey");
                            texturechoice = getInt("Choose soil texture: ", keyboard, 1, 3);
                            switch(texturechoice){
                                case 1 -> textureheader = "Sandy";
                                case 2 -> textureheader = "Loamy";
                                case 3 -> textureheader = "Clayey";
                            }
                        } else {
                            System.out.println("Available Bed Materials: ");
                            System.out.println("[1] Wood");
                            System.out.println("[2] Concrete");
                            System.out.println("[3] Metal");
                            materialchoice = getInt("Choose bed material: ", keyboard, 1, 3);
                            switch(materialchoice){
                                case 1 -> materialheader = "Wood";
                                case 2 -> materialheader = "Concrete";
                                case 3 -> materialheader = "Metal";
                            }
                        }
                        break;
                    case 4:
                        if(typechoice == 1){
                            ph = getBigDecimal("PH Level (Leave blank if unassessed): ", keyboard, false, BigDecimal.ZERO);
                        } else {
                            height = getBigDecimal("Bed Height (cm): ", keyboard, true, new BigDecimal("0.01"));
                        }
                        break;
                }
            }
            counter++;
        }
        String typeCode = (typechoice == 1) ? "GND" : "RSD";
        String texCode = switch (texturechoice) {
            case 1 -> "SDY";
            case 2 -> "LMY";
            case 3 -> "CLY";
            default -> null;
        };
        String matCode = switch (materialchoice) {
            case 1 -> "WOD";
            case 2 -> "CON";
            case 3 -> "MET";
            default -> null;
        };
        plotCRUD.createPlot(loc, area, typeCode, texCode, ph, matCode, height);
    }

    public static void optionPlotManagement(Scanner keyboard) {
        int counter = 0;
        String plotFilter = "";
        String locFilter = "";
        int typeChoice = 3;
        String typeHeader = "All";
        while (counter < 3) {
            System.out.println("\n=======================================================");
            System.out.println("              SEARCH & SELECT A PLOT");
            System.out.println("=======================================================");
            if (counter >= 1) {
                System.out.println("    Plot Filter     : " + (plotFilter.isEmpty() ? "All" : plotFilter));
                if (counter >= 2) {
                    System.out.println("    Location Filter : " + (locFilter.isEmpty() ? "All" : locFilter));
                }
                System.out.println("=======================================================");
            }
            switch (counter) {
                case 0:
                    plotFilter = getString("Filter by Plot Code (Leave blank for all): ", keyboard, false).toUpperCase();
                    break;
                case 1:
                    locFilter = getString("Filter by Location (Leave blank for all): ", keyboard, false);
                    break;
                case 2:
                    System.out.println("Available Plot Types: ");
                    System.out.println("[1] Ground");
                    System.out.println("[2] Raised");
                    System.out.println("[3] All");
                    typeChoice = getInt("Choose plot type filter: ", keyboard, 1, 3);
                    typeHeader = switch (typeChoice) {
                        case 1 -> "Ground";
                        case 2 -> "Raised";
                        default -> "All";
                    };
                    break;
            }
            counter++;
        }
        boolean isEmpty = plotCRUD.viewPlots(plotFilter, locFilter, typeHeader);
        if (isEmpty) {
            return;
        }
        String selectedPlot = getString("Enter Plot Code to open (Leave blank to cancel): ", keyboard, false).toUpperCase();
        if (selectedPlot.isEmpty()) {
            return;
        }
        int loadedRevNo = plotCRUD.loadPlot(selectedPlot);
        if (loadedRevNo != -1) {
            selectedPlotMenu(selectedPlot, loadedRevNo, keyboard);
        }
    }

    public static void selectedPlotMenu(String plotCode, int initialRevNo, Scanner keyboard) {
        int currentRevNo = initialRevNo;
        while (true) {
            String typeCode = plotCRUD.getPlotTypeCode(plotCode);
            if (typeCode == null){
                return;
            }
            boolean isRaised = "RSD".equalsIgnoreCase(typeCode);
            System.out.println("\n=======================================================");
            System.out.println("    SELECTED PLOT: " + plotCode + " (" + (isRaised ? "Raised Bed" : "Ground Plot") + ")");
            System.out.println("=======================================================");
            System.out.println(" [1] Update Plot Details");
            System.out.println(" [2] Delete This Plot");
            System.out.println(" [3] Manage Assignments for This Plot");
            if (isRaised) {
                System.out.println(" [4] Manage Structural Repairs for This Bed");
            }
            System.out.println(" [0] Back to Main Menu");
            System.out.println("=======================================================");
            int maxChoice = isRaised ? 4 : 3;
            int choice = getInt("Choose an action: ", keyboard, 0, maxChoice);
            if (choice == 0) {
                break;
            }
            switch (choice) {
                case 1:
                    updatePlotMenu(keyboard, plotCode, currentRevNo, isRaised);
                    break;
                case 2: 
                    boolean confirm = getConfirmation("Confirm permanent deletion of plot '" + plotCode + "'? (Y/N): ", keyboard);
                    if (confirm) {
                        plotCRUD.deletePlot(plotCode);
                        if (plotCRUD.getPlotTypeCode(plotCode) == null){
                            return;
                        } 
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;
                case 3:
                    manageAssignmentMenu(keyboard, plotCode);
                    break;
                case 4:
                    manageRepairMenu(plotCode, keyboard);
                    break;
            }
            if (choice != 2){
                int refreshed = plotCRUD.loadPlot(plotCode);
                if (refreshed != -1) {
                    currentRevNo = refreshed;
                }
            }
        }
    }

    public static void updatePlotMenu(Scanner keyboard, String plotCode, int currentRevNo, boolean isRaised){
        int counter = 0;
        String newLoc = "";
        String header3 = "";
        String newTex = null;
        String newMat = null;
        String value = "";
        BigDecimal newHeight = null;
        BigDecimal newPH = null;
        BigDecimal newArea = BigDecimal.ZERO;
        String header4 = "";
        while(counter < 5){
            System.out.println("\n=======================================================");
            System.out.println("    UPDATE PLOT: " + plotCode);
            System.out.println("=======================================================");
            if(counter >= 1){
                System.out.println(" New Location: " + newLoc);
                if(counter >= 2){
                    System.out.println(" New Area: " + newArea);
                    if (counter >= 3){
                        System.out.println(header3);
                        if(counter >= 4){
                            System.out.println(header4);
                        }
                    }
                }
                System.out.println("=======================================================");
            }
            switch (counter){
                case 0:
                    newLoc = getString("New Plot Location: ", keyboard, true);
                    break;
                case 1:
                    newArea = getBigDecimal("New Usable Area (sq. m): ", keyboard, true, new BigDecimal("0.01"));
                    break;
                case 2:
                    if (!isRaised) {
                        System.out.println("Available Soil Textures:");
                        System.out.println(" [1] Sandy");
                        System.out.println(" [2] Loamy");
                        System.out.println(" [3] Clayey");
                        int texChoice = getInt("Choose new soil texture: ", keyboard, 1, 3);
                        newTex = switch (texChoice) { 
                                    case 1 -> "SDY"; 
                                    case 2 -> "LMY"; 
                                    default -> "CLY"; 
                                };
                        value = switch (texChoice) { 
                                    case 1 -> "Sandy"; 
                                    case 2 -> "Loamy"; 
                                    default -> "Clayey"; 
                                };
                        header3 = " New Soil Texture: " + value;
                    } else {
                        System.out.println("Available Bed Materials:");
                        System.out.println(" [1] Wood");
                        System.out.println(" [2] Concrete");
                        System.out.println(" [3] Metal");
                        int matChoice = getInt("Choose new bed material: ", keyboard, 1, 3);
                        newMat = switch (matChoice) { 
                                    case 1 -> "WOD"; 
                                    case 2 -> "CON"; 
                                    default -> "MET"; 
                                };
                        value = switch (matChoice) { 
                                    case 1 -> "Wood"; 
                                    case 2 -> "Concrete"; 
                                    default -> "Metal"; 
                                };
                        header3 = " New Bed Material: " + value;
                    }
                    break;
                case 3:
                    if(!isRaised){
                        newPH = getBigDecimal("New PH Level (Leave blank if unassessed): ", keyboard, false, BigDecimal.ZERO);
                        header4 = " New pH Level: " + newPH;
                    } else {
                        newHeight = getBigDecimal("New Bed Height (cm): ", keyboard, true, new BigDecimal("0.01"));
                        header4 = " New Height (cm): " + newHeight;
                    }
                    break;
                case 4:
                    boolean comfirn = getConfirmation("Confirm Plot Update? (Y/N):", keyboard);
                    if(comfirn){
                        plotCRUD.updatePlot(plotCode, newLoc, newArea, newTex, newPH, newMat, newHeight, currentRevNo);
                    } else {
                        System.out.println("Plot Update Cancelled!");
                    }
            }
            counter++;
        }
    }

    public static void manageAssignmentMenu(Scanner keyboard, String plotCode) {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("    MANAGE ASSIGNMENTS FOR PLOT: " + plotCode);
            System.out.println("=======================================================");
            System.out.println(" [1] Search / View Assignment Listing for This Plot");
            System.out.println(" [2] Assign Plot to a Household");
            System.out.println(" [3] Update an Assignment");
            System.out.println(" [4] Release an Assignment");
            System.out.println(" [5] Delete an Unused Assignment");
            System.out.println(" [0] Back to Selected Plot");
            System.out.println("=======================================================");
            int choice = getInt("Choose an action: ", keyboard, 0, 5);
            if (choice == 0) {
                break;
            }
            switch (choice) {
                case 1:
                    int counter = 0;
                    String assignFilter = "";
                    String householdFilter = "";
                    LocalDate startFrom = null;
                    LocalDate startTo = null;
                    while (counter < 4) {
                        System.out.println("\n=======================================================");
                        System.out.println("    SEARCH ASSIGNMENTS ON PLOT: " + plotCode);
                        System.out.println("=======================================================");
                        if (counter >= 1) {
                            System.out.println("    Assign Code     : " + (assignFilter.isEmpty() ? "All" : assignFilter));
                            if (counter >= 2) {
                                System.out.println("    Household Label : " + (householdFilter.isEmpty() ? "All" : householdFilter));
                                if (counter >= 3) {
                                    System.out.println("    Start Date From : " + (startFrom == null ? "Any" : startFrom));
                                }
                            }
                            System.out.println("=======================================================");
                        }
                        switch (counter) {
                            case 0: 
                                assignFilter = getString("Filter by Assignment Code (Leave blank for all): ", keyboard, false).toUpperCase();
                                break;
                            case 1: 
                                householdFilter = getString("Filter by Household Label (Leave blank for all): ", keyboard, false);
                                break;
                            case 2: 
                                startFrom = getDate("Start Date From (YYYY-MM-DD, Leave blank for any): ", keyboard, false);
                                break;
                            case 3 : 
                                while (true){
                                    startTo = getDate("Start Date To (YYYY-MM-DD, Leave blank for any): ", keyboard, false);
                                    if (startTo == null || startFrom == null || !startTo.isBefore(startFrom)) {
                                        break;
                                    }
                                    System.out.println("ERROR: End date cannot precede start date (" + startFrom + ").");
                                }
                                break;
                        }
                        counter++;
                    }
                    assignCRUD.viewAssignments(assignFilter, householdFilter, plotCode, startFrom, startTo);
                    break;
                case 2:
                    int hhCount = 0;
                    String hhCode = "";
                    LocalDate start = null;
                    LocalDate end = null;
                    String remarks = "";
                    while (hhCount < 4) {
                        System.out.println("\n=======================================================");
                        System.out.println("    ASSIGN PLOT " + plotCode + " TO A HOUSEHOLD");
                        if (hhCount >= 1) {
                            System.out.println("=======================================================");
                            System.out.println("    Household Code : " + hhCode);
                            if (hhCount >= 2) {
                                System.out.println("    Start Date     : " + start);
                                if (hhCount >= 3) {
                                    System.out.println("    End Date       : " + (end == null ? "Ongoing" : end));
                                }
                            }
                            System.out.println("=======================================================");
                        }
                        switch (hhCount) {
                            case 0:
                                assignCRUD.printHouseholds();
                                hhCode = getString("Enter Household Code (HXXX): ", keyboard, true).toUpperCase();
                                break;
                            case 1:
                                start = getDate("Start Date (YYYY-MM-DD): ", keyboard, true);
                                break;
                            case 2:
                                while (true) {
                                    end = getDate("End Date (YYYY-MM-DD, Leave blank if open-ended): ", keyboard, false);
                                    if (end == null || !end.isBefore(start)) {
                                        break;
                                    }
                                    System.out.println("ERROR: End date cannot precede start date (" + start + ").");
                                }
                                break;
                            case 3:
                                remarks = getString("Remarks (Leave blank if none): ", keyboard, false);
                                break;
                        }
                        hhCount++;
                    }
                    assignCRUD.createAssignment(hhCode, plotCode, start, end, remarks);
                    break;
                case 3:
                    if (assignCRUD.viewAssignments(null, null, plotCode, null, null)){
                        break;
                    }
                    String aCode = getString("Enter Assignment Code to correct: ", keyboard, true).toUpperCase();
                    int assignRev = assignCRUD.loadAssignment(aCode);
                    if (assignRev == -1) {
                        break;
                    }
                    int updateCount = 0;
                    LocalDate newStart = null;
                    LocalDate newEnd = null;
                    String newRemarks = "";
                    while (updateCount < 4) {
                        System.out.println("\n=======================================================");
                        System.out.println("    UPDATE ASSIGNMENT: " + aCode);
                        System.out.println("=======================================================");
                        if (updateCount >= 1) {
                            System.out.println("    New Start Date : " + newStart);
                            if (updateCount >= 2) {
                                System.out.println("    New End Date   : " + (newEnd == null ? "Ongoing" : newEnd));
                                if (updateCount >= 3){
                                    System.out.println("    New Remarks   : " + (newRemarks == null ? "None" : newRemarks));
                                }
                            }
                            System.out.println("=======================================================");
                        }
                        switch (updateCount) {
                            case 0: 
                                newStart = getDate("New Start Date (YYYY-MM-DD): ", keyboard, true);
                                break;
                            case 1: 
                                while (true) {
                                    newEnd = getDate("New End Date (YYYY-MM-DD, Leave blank if open-ended): ", keyboard, false);
                                    if (newEnd == null || !newEnd.isBefore(newStart)) {
                                        break;
                                    }
                                    System.out.println("ERROR: End date cannot precede start date (" + newStart + ").");
                                }
                                break;
                            case 2: 
                                newRemarks = getString("New Remarks (Leave blank if none): ", keyboard, false);
                                break;
                            case 3: 
                                boolean updateconfirm = getConfirmation("Confirm assignment details? (Y/N): ", keyboard);
                                if(updateconfirm){
                                    assignCRUD.updateAssignment(aCode, newStart, newEnd, newRemarks, assignRev);
                                } else {
                                    System.out.println("Assignment update cancelled!");
                                }
                        }
                        updateCount++;
                    }
                    break;
                case 4:
                    if (assignCRUD.viewAssignments(null, null, plotCode, null, null)){
                        break;
                    }
                    String toRelease = getString("Enter Assign Code to release: ", keyboard, true).toUpperCase();
                    int releaseRev = assignCRUD.loadAssignment(toRelease);
                    if (releaseRev == -1) {
                        break;
                    }
                    int rCount = 0;
                    LocalDate startDate = assignCRUD.getAssignmentStartDate(toRelease);
                    LocalDate endDate = null;
                    String releaseRemarks = "";
                    boolean releaseConfirm = false;
                    while (rCount < 3){
                        if(rCount > 0) {
                            System.out.println("\n=======================================================");
                        }
                        System.out.println("    Assignment To Release: " + toRelease);
                        System.out.println("    Start Date: " + startDate);
                        System.out.println("=======================================================");
                        if(rCount >= 1){
                            System.out.println("    Release Date: " + endDate);
                            if (rCount >= 2){
                                System.out.println("    Remarks: " + releaseRemarks);
                            }
                            System.out.println("=======================================================");
                        }
                        switch (rCount) {
                            case 0:
                                endDate = getDate("Release Date (YYYY-MM-DD): ", keyboard, true);
                                if (endDate.isBefore(startDate)){
                                    System.out.println("ERROR: Invalid release date as it cannot precede start date (" + startDate + ").");
                                    rCount--;
                                }
                                break;
                            case 1:
                                releaseRemarks = getString("Release Remarks (Leave blank if none): ", keyboard, false);
                                break;
                            case 2:
                                releaseConfirm = getConfirmation("Confirm release of '" + toRelease + "'? (Y/N): ", keyboard);
                                if(releaseConfirm){
                                    assignCRUD.releaseAssignment(toRelease, endDate, releaseRemarks, releaseRev);
                                } else {
                                    System.out.println("Assignment Release Cancelled.");
                                }
                                break;
                        }
                        rCount++;
                    }
                    break;
                case 5:
                    if (assignCRUD.viewAssignments(null, null, plotCode, null, null)) {
                        break;
                    }
                    String toDelete = getString("Enter Unused Assign Code to delete: ", keyboard, true).toUpperCase();
                    if(assignCRUD.loadAssignment(toDelete) == -1){
                        break;
                    }
                    boolean confirm = getConfirmation("Confirm deletion of assignment '" + toDelete + "'? (Y/N): ", keyboard);
                    if (confirm) {
                        assignCRUD.deleteAssignment(toDelete);
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;
            }
        }
    }

    public static void manageRepairMenu(String plotCode, Scanner keyboard) {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("    MANAGE STRUCTURAL REPAIRS FOR BED: " + plotCode);
            System.out.println("=======================================================");
            System.out.println(" [1] Search / View Repair Listing for This Bed");
            System.out.println(" [2] Log New Structural Repair");
            System.out.println(" [3] Update a Repair Entry");
            System.out.println(" [4] Delete a Repair Entry");
            System.out.println(" [0] Back to Selected Plot");
            System.out.println("=======================================================");
            int choice = getInt("Choose an action: ", keyboard, 0, 4);
            if (choice == 0) {
                break;
            }
            switch (choice) {
                case 1:
                    int counter = 0;
                    String rFilter = "";
                    LocalDate dateFrom = null;
                    LocalDate dateTo = null;
                    while (counter < 3) {
                        System.out.println("\n=======================================================");
                        System.out.println("    SEARCH REPAIRS ON RAISED BED: " + plotCode);
                        System.out.println("=======================================================");
                        if (counter >= 1) {
                            System.out.println("    Repair Code      : " + (rFilter.isEmpty() ? "All" : rFilter));
                            if (counter >= 2) {
                                System.out.println("    Repair Date From : " + (dateFrom == null ? "Any" : dateFrom));
                            }
                            System.out.println("=======================================================");
                        }
                        switch (counter) {
                            case 0:
                                rFilter = getString("Filter by Repair Code (Leave blank for all): ", keyboard, false).toUpperCase();
                                break;
                            case 1:
                                dateFrom = getDate("Repair Date From (YYYY-MM-DD, Leave blank for any): ", keyboard, false);
                                break;
                            case 2:
                                while (true) {
                                    dateTo = getDate("Repair Date To (YYYY-MM-DD, Leave blank for any): ", keyboard, false);
                                    if (dateTo == null || dateFrom == null || !dateTo.isBefore(dateFrom)) {
                                        break;
                                    }
                                    System.out.println("ERROR: End range date cannot precede Repair Date (" + dateFrom + ").");
                                }
                                break;
                        }
                        counter++;
                    }
                    repairCRUD.viewRepairs(rFilter, plotCode, dateFrom, dateTo);
                    break;
                case 2:
                    int addCount = 0;
                    LocalDate repDate = null;
                    String workDesc = "";
                    BigDecimal cost = null;
                    while (addCount < 4) {
                        System.out.println("\n=======================================================");
                        System.out.println("    LOG STRUCTURAL REPAIR FOR BED: " + plotCode);
                        System.out.println("=======================================================");
                        if (addCount >= 1) {
                            System.out.println("    Repair Date : " + repDate);
                            if (addCount >= 2) {
                                System.out.println("    Work Desc   : " + workDesc);
                                if (addCount >= 3) {
                                    System.out.println("    Expense     : " + (cost == null ? "(Unknown)" : "PHP " + cost.toPlainString()));
                                }
                            }
                            System.out.println("=======================================================");
                        }
                        switch (addCount) {
                            case 0:
                                while (true) {
                                    repDate = getDate("Repair Date (YYYY-MM-DD): ", keyboard, true);
                                    if (!repDate.isAfter(LocalDate.now())) {
                                        break;
                                    }
                                    System.out.println("ERROR: Repair date cannot be in the future (" + LocalDate.now() + ").");
                                }
                                break;
                            case 1:
                                workDesc = getString("Completed Work Description: ", keyboard, true);
                                break;
                            case 2:
                                cost = getBigDecimal("Expense in PHP (Leave blank if unknown, 0 if free): ", keyboard, false, BigDecimal.ZERO);
                                break;
                            case 3:
                                boolean saveConfirm = getConfirmation("Confirm logging this repair? (Y/N): ", keyboard);
                                if (saveConfirm) {
                                    repairCRUD.createRepair(plotCode, repDate, workDesc, cost);
                                } else {
                                    System.out.println("Repair logging cancelled.");
                                }
                                break;
                        }
                        addCount++;
                    }
                    break;

                case 3:
                    if (repairCRUD.viewRepairs(null, plotCode, null, null)) {
                        break;
                    }
                    String rCode = getString("Enter Repair Code to update: ", keyboard, true).toUpperCase();
                    int repRev = repairCRUD.loadRepair(rCode);
                    if (repRev == -1) {
                        break;
                    }
                    int editCount = 0;
                    LocalDate newDate = null;
                    String newDesc = "";
                    BigDecimal newCost = null;
                    while (editCount < 4) {
                        System.out.println("\n=======================================================");
                        System.out.println("    UPDATE REPAIR ENTRY: " + rCode);
                        System.out.println("=======================================================");
                        if (editCount >= 1) {
                            System.out.println("    New Repair Date : " + newDate);
                            if (editCount >= 2) {
                                System.out.println("    New Work Desc   : " + newDesc);
                                if (editCount >= 3) {
                                    System.out.println("    New Expense (PHP): " + (newCost == null ? "(Unknown)" : "PHP " + newCost.toPlainString()));
                                }
                            }
                            System.out.println("=======================================================");
                        }
                        switch (editCount) {
                            case 0:
                                while (true) {
                                    newDate = getDate("New Repair Date (YYYY-MM-DD): ", keyboard, true);
                                    if (!newDate.isAfter(LocalDate.now())) {
                                        break;
                                    }
                                    System.out.println("ERROR: Repair date cannot be in the future from now (" + LocalDate.now() + ").");
                                }
                                break;
                            case 1:
                                newDesc = getString("New Completed Work Description: ", keyboard, true);
                                break;
                            case 2:
                                newCost = getBigDecimal("New Expense in PHP (Leave blank if unknown): ", keyboard, false, BigDecimal.ZERO);
                                break;
                            case 3:
                                boolean editConfirm = getConfirmation("Confirm repair update? (Y/N): ", keyboard);
                                if (editConfirm) {
                                    repairCRUD.updateRepair(rCode, newDate, newDesc, newCost, repRev);
                                } else {
                                    System.out.println("Repair update cancelled.");
                                }
                                break;
                        }
                        editCount++;
                    }
                    break;

                case 4:
                    if (repairCRUD.viewRepairs(null, plotCode, null, null)) {
                        break;
                    }
                    String toDelete = getString("Enter Repair Code to delete: ", keyboard, true).toUpperCase();
                    if (repairCRUD.loadRepair(toDelete) == -1) {
                        break;
                    }
                    boolean delConfirm = getConfirmation("Confirm deletion of repair '" + toDelete + "'? (Y/N): ", keyboard);
                    if (delConfirm) {
                        repairCRUD.deleteRepair(toDelete);
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;
            }
        }
    }

    public static void optionReport(Scanner keyboard) {
        int counter = 0;
        LocalDate startDate = null;
        LocalDate endDate = null;
        String plotCode = "";
        int typeChoice = 3;
        String typeHeader = "All";
        int minStarts = 0;
        while (counter < 5) {
            System.out.println("\n=======================================================");
            System.out.println("           REPORT 2: PLOT ALLOCATION STARTS");
            System.out.println("=======================================================");
            if (counter >= 1) {
                System.out.println("    Period Start : " + startDate);
                if (counter >= 2) {
                    System.out.println("    Period End   : " + endDate);
                    if (counter >= 3) {
                        System.out.println("    Plot Filter  : " + (plotCode.isEmpty() ? "All" : plotCode));
                        if (counter >= 4) {
                            System.out.println("    Plot Type    : " + typeHeader);
                        }
                    }
                }
                System.out.println("=======================================================");
            }
            switch (counter) {
                case 0 : 
                    startDate = getDate("Period Start (YYYY-MM-DD): ", keyboard, true);
                    break;
                case 1: 
                    while (true) {
                        endDate = getDate("Period End (YYYY-MM-DD): ", keyboard, true);
                        if (!endDate.isBefore(startDate)) {
                            break;
                        }
                        System.out.println("ERROR: End date cannot precede start date (" + startDate + ").");
                    }
                    break;
                case 2: 
                    plotCode = getString("Filter by Plot Code (Leave blank for all): ", keyboard, false).toUpperCase();
                    break;
                case 3: 
                    System.out.println("Available Plot Types: ");
                    System.out.println("[1] Ground");
                    System.out.println("[2] Raised");
                    System.out.println("[3] All");
                    typeChoice = getInt("Choose plot type filter: ", keyboard, 1, 3);
                    typeHeader = switch (typeChoice) {
                        case 1 -> "Ground";
                        case 2 -> "Raised";
                        default -> "All";
                    };
                    break;
                case 4:
                    minStarts = getInt("Minimum Assignment Starts (0 or higher): ", keyboard, 0, Integer.MAX_VALUE);
                    break;
            }
            counter++;
        }
        report2.generateReport(startDate, endDate, plotCode, typeHeader, minStarts);
    }

    public static String getString(String prompt, Scanner keyboard, boolean mustBeFilled){
        String input = "";
        while(true){
            System.out.print(prompt);
            input = keyboard.nextLine();
            if(mustBeFilled && input.isBlank()){
                System.out.println("ERROR: Missing input!");   
            } else {
                break;
            }
        }
        return input.trim();
    }

    public static int getInt(String prompt, Scanner keyboard, int min, int max){
        int choice = -1;
        while(true){
            try{
                String input = getString(prompt, keyboard, true);
                choice = Integer.parseInt(input);
                if(choice >= min && choice <= max){
                    break;
                } else {
                    System.out.println("ERROR: Invalid choice. Please put a number between " + min + " and " + max + "!");
                }
            } catch(NumberFormatException e){
                System.out.println("ERROR: Input must be a valid integer!");
            }   
        }
        return choice;
    }

    public static BigDecimal getBigDecimal(String prompt, Scanner keyboard, boolean mustBeFilled, BigDecimal min){
        while (true) {
            try {
                String input = getString(prompt, keyboard, mustBeFilled);
                if (!mustBeFilled && input.isEmpty()) {
                    return null;
                }
                BigDecimal output = new BigDecimal(input);
                if(min != null && output.compareTo(min) < 0){
                    System.out.println("ERROR: Value must be at least " + min.toPlainString() + "!");
                    continue;
                }
                return output; 
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Input must be a valid number!");
            }
        }
    }

    public static LocalDate getDate(String prompt, Scanner keyboard, boolean mustBeFilled) {
        while (true) {
            try {
                String input = getString(prompt, keyboard, mustBeFilled);
                if (!mustBeFilled && input.isEmpty()) {
                    return null;
                }
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("ERROR: Invalid date! Please use YYYY-MM-DD format.");
            }
        }
    }

    public static boolean getConfirmation(String prompt, Scanner keyboard){
        while(true){
            String input = getString(prompt, keyboard, true);
            if(input.equalsIgnoreCase("Y")){
                return true;
            } else if (input.equalsIgnoreCase("N")){
                return false;
            } else {
                System.out.println("ERROR: Input must be Y or N!");
            }
        }
    }
}