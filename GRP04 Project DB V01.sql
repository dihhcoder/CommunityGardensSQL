CREATE DATABASE IF NOT EXISTS G04_V01;
USE G04_V01;

CREATE TABLE ZONES (
    zone_code VARCHAR(5) PRIMARY KEY,
    zone_name VARCHAR(100) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE HOUSEHOLDS (
	hh_code VARCHAR(5) PRIMARY KEY,
    reg_date DATE NOT NULL,
    hh_label VARCHAR(100) NOT NULL,
    contact VARCHAR(100) NOT NULL,
    mobile_num	VARCHAR(24) NULL,
    address	VARCHAR(100) NOT NULL,
    zone_code VARCHAR(5) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (zone_code) REFERENCES ZONES(zone_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE TYPES_LOOKUP (
	type_code VARCHAR(5) PRIMARY KEY,
	plot_type VARCHAR(50) NOT NULL,
    plot_desc VARCHAR(255) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE TEXTURES (
	tex_code VARCHAR(5) PRIMARY KEY,
    soil_tex VARCHAR(50) NOT NULL,
    tex_desc VARCHAR(255) NOT NULL,
    tex_guide VARCHAR(255) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE MATERIALS (
	mat_code VARCHAR(5) PRIMARY KEY,
    bed_mat VARCHAR(50) NOT NULL,
    mat_desc VARCHAR(255) NOT NULL,
    mat_guide VARCHAR(255) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE PLOTS (
    plot_code VARCHAR(5) PRIMARY KEY,
    plot_location VARCHAR(100) NOT NULL,
    plot_area DECIMAL(8,2) NOT NULL,
    type_code VARCHAR(5) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (type_code) REFERENCES TYPES_LOOKUP(type_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (plot_area > 0),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE RAISED_BEDS (
	plot_code VARCHAR(5) PRIMARY KEY,
    mat_code VARCHAR(5) NOT NULL,
    bed_height DECIMAL(8,2) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (plot_code) REFERENCES PLOTS(plot_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (mat_code) REFERENCES MATERIALS(mat_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (bed_height > 0),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE GROUND_PLOTS (
	plot_code VARCHAR(5) PRIMARY KEY,
    tex_code VARCHAR(5) NOT NULL,
    ph_level DECIMAL(4,2) NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (plot_code) REFERENCES PLOTS(plot_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (tex_code) REFERENCES TEXTURES(tex_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (ph_level IS NULL OR ph_level BETWEEN 0 AND 14),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE BED_REPAIRS (
	repair_code VARCHAR(5) PRIMARY KEY,
    plot_code VARCHAR(5) NOT NULL,
    repair_date DATE NOT NULL,
    work_desc VARCHAR(255) NOT NULL,
    cost DECIMAL(10,2) NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (plot_code) REFERENCES RAISED_BEDS(plot_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (cost IS NULL OR cost >= 0),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE ASSIGNMENTS (
	assign_code VARCHAR(5) PRIMARY KEY,
    hh_code VARCHAR(5) NOT NULL,
    plot_code VARCHAR(5) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    remarks VARCHAR(255),
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (hh_code) REFERENCES HOUSEHOLDS(hh_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (plot_code) REFERENCES PLOTS(plot_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (end_date IS NULL OR end_date >= start_date),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE METHODS (
	met_code VARCHAR(5) PRIMARY KEY,
	method VARCHAR(50) NOT NULL,
    met_desc VARCHAR(255) NOT NULL,
    met_guide VARCHAR(255) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE CROPS (
	crop_code VARCHAR(5) PRIMARY KEY,
    crop_name VARCHAR(50) NOT NULL,
    variety VARCHAR(255) NOT NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE PLANTS (
	plant_code VARCHAR(5) PRIMARY KEY,
    assign_code VARCHAR(5),
    crop_code VARCHAR(5),
    plant_date DATE NOT NULL,
    met_code VARCHAR(5) NOT NULL,
    plant_area DECIMAL(5,2) NOT NULL,
    close_date DATE NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    FOREIGN KEY (assign_code) REFERENCES ASSIGNMENTS(assign_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (met_code) REFERENCES METHODS(met_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (crop_code) REFERENCES CROPS(crop_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (plant_area > 0),
    CHECK (close_date IS NULL OR close_date >= plant_date),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE HARVESTS (
	plant_code VARCHAR(5) NOT NULL,
    harvest_seq INT UNSIGNED NOT NULL,
    harvest_date DATE NOT NULL,
    qty_kg DECIMAL(9,2) NOT NULL,
    remarks VARCHAR(255) NULL,
    rev_no	INT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY(plant_code, harvest_seq),
    FOREIGN KEY (plant_code) REFERENCES PLANTS(plant_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (harvest_seq > 0),
	CHECK (qty_kg > 0),
    CHECK (rev_no >= 1)
) ENGINE=InnoDB;


-- GENERATED DATA POPULATION SCRIPT
USE G04_V01;

-- ==========================================
-- STEP 1: MASTER LOOKUP TABLES (No Dependencies)
-- ==========================================

-- Populate zone
INSERT INTO ZONES (zone_code, zone_name, rev_no) VALUES
('Z01', 'North Quadrant Allotments', 1),
('Z02', 'East Valley Community Garden', 1),
('Z03', 'South Riverside Plots', 1);

-- Populate typedesc
INSERT INTO TYPES_LOOKUP (type_code, plot_type, plot_desc, rev_no) VALUES
('GND', 'Ground', 'Traditional open soil farming plots on ground level', 1),
('RSD', 'Raised', 'Constructed container framing beds elevated above ground level', 1);

-- Populate texdesc
INSERT INTO TEXTURES (tex_code, soil_tex, tex_desc, tex_guide, rev_no) VALUES
('SDY', 'Sandy', 'Coarse-textured soil with high drainage capacity', 'Requires frequent watering and organic matter supplementation.', 1),
('LMY', 'Loamy', 'Balanced soil mixture of sand, silt, and clay', 'Ideal for most vegetables. Maintain regular mulching.', 1),
('CLY', 'Clayey', 'Fine-textured dense soil with high nutrient retention', 'Aerate regularly to prevent compaction and waterlogging.', 1);

-- Populate matdesc
INSERT INTO MATERIALS (mat_code, bed_mat, mat_desc, mat_guide, rev_no) VALUES
('WOD', 'Wood', 'Untreated cedar or redwood structural frames', 'Inspect annually for localized rot or structural weakening.', 1),
('CON', 'Concrete', 'Pre-cast concrete block or poured wall perimeters', 'Highly durable; check for cracks or shifting post-winter.', 1),
('MET', 'Metal', 'Corrugated galvanized steel containment structures', 'Ensure protective edge linings remain intact to prevent cuts.', 1);

-- Populate metdesc
INSERT INTO METHODS (met_code, method, met_desc, met_guide, rev_no) VALUES
('DIR', 'Direct', 'Sowing seeds directly into the permanent plot soil', 'Sow at recommended seed depths; keep soil consistently moist until sprout.', 1),
('TRN', 'Transplant', 'Moving young nursery seedlings into the target field plot', 'Harden off seedlings for 7 days before outdoor transplantation.', 1);

-- Populate crops
INSERT INTO CROPS (crop_code, crop_name, variety, rev_no) VALUES
('C001', 'Tomato', 'Roma VF Determinate', 1),
('C002', 'Lettuce', 'Grand Rapids Looseleaf', 1),
('C003', 'Carrot', 'Nantes Coreless Heirloom', 1);


-- ==========================================
-- STEP 2: MASTER ENTITIES (Dependent only on Lookups)
-- ==========================================

-- Populate household
INSERT INTO HOUSEHOLDS (hh_code, reg_date, hh_label, contact, mobile_num, address, zone_code, rev_no) VALUES
('H001', '2026-01-15', 'Santos Family Household', 'Juan Santos', '+639171234567', '124 Rizal Avenue', 'Z01', 1),
('H002', '2026-02-10', 'Cruz Agricultural Cohort', 'Maria Cruz', '+639189876543', '45 Mabini Street', 'Z02', 1),
('H003', '2026-03-01', 'Reyes Urban Homestead', 'Robert Reyes', NULL, '78 Dynamic Lane', 'Z03', 1);

-- NEW ARRANGE ELEMENT: Populate the superclass plot table FIRST!
INSERT INTO PLOTS (plot_code, plot_location, plot_area, type_code, rev_no) VALUES
('P001', 'Sector A - Row 1', 15.25, 'GND', 1),
('P002', 'Sector A - Row 2', 20.00, 'GND', 1),
('P003', 'Terrace B - Bed 1', 6.00, 'RSD', 1),
('P004', 'Terrace B - Bed 2', 8.50, 'RSD', 1);


-- ==========================================
-- STEP 3: SUBCLASSES (Dependent on Superclass plot)
-- ==========================================

-- Populate groundplot (Looks up to plot)
INSERT INTO GROUND_PLOTS (plot_code, tex_code, ph_level, rev_no) VALUES
('P001', 'LMY', 6.50, 1),
('P002', 'SDY', 5.80, 1);

-- Populate raisedbed (Looks up to plot)
INSERT INTO RAISED_BEDS (plot_code, mat_code, bed_height, rev_no) VALUES
('P003', 'WOD', 30.50, 1),
('P004', 'CON', 45.00, 1);


-- ==========================================
-- STEP 4: TRANSACTIONAL HISTORIES & LOGS
-- ==========================================

-- Populate bedrepair (Looks up to raisedbed subclass)
INSERT INTO BED_REPAIRS (repair_code, plot_code, rep_date, work_desc, cost, rev_no) VALUES
('R001', 'P003', '2026-05-12', 'Replaced degraded structural corner bracket', 250.00, 1),
('R002', 'P004', '2026-08-20', 'Sealed cosmetic hair cracks on outer concrete face', NULL, 1);

-- Populate assignment (Looks up to plot master)
INSERT INTO ASSIGNMENTS (assign_code, hh_code, plot_code, start_date, end_date, remarks, rev_no) VALUES
('A001', 'H001', 'P001', '2026-02-01', NULL, 'Primary leafy green production area', 1),
('A002', 'H002', 'P003', '2026-02-15', '2026-06-15', 'Short term allocation for root trial', 1),
('A003', 'H003', 'P004', '2026-03-10', NULL, 'Assigned to active urban gardening program', 1);

-- Populate planting
INSERT INTO PLANTS (plant_code, assign_code, crop_code, plant_date, met_code, plant_area, close_date, rev_no) VALUES
('PL01', 'A001', 'C002', '2026-02-05', 'DIR', 10.50, '2026-04-10', 1),
('PL02', 'A002', 'C003', '2026-02-20', 'DIR', 5.00, '2026-06-01', 1),
('PL03', 'A003', 'C001', '2026-03-15', 'TRN', 8.00, NULL, 1);

-- Populate harvest
INSERT INTO HARVESTS (plant_code, harvest_seq, harvest_date, qty_kg, remarks, rev_no) VALUES
('PL01', 1, '2026-03-15', 12.45, 'First crop thinning yield', 1),
('PL01', 2, '2026-04-05', 28.30, 'Main structural harvest completed', 1),
('PL02', 1, '2026-05-28', 42.10, 'High quality uniform yield realized', 1);
