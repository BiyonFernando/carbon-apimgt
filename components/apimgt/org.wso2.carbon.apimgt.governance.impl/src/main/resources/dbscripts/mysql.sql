CREATE TABLE IF NOT EXISTS GOV_RULESET (
    RULESET_ID VARCHAR(36) NOT NULL,
    NAME VARCHAR(256) NOT NULL,
    DESCRIPTION VARCHAR(1024),
    RULESET_CONTENT LONGBLOB NOT NULL,
    ARTIFACT_TYPE VARCHAR(128) NOT NULL,
    RULE_CATEGORY VARCHAR(128) NOT NULL,
    RULE_TYPE VARCHAR(128) NOT NULL,
    DOCUMENTATION_LINK VARCHAR(1024),
    PROVIDER VARCHAR(256),
    ORGANIZATION VARCHAR(128) NOT NULL,
    CREATED_BY VARCHAR(256),
    CREATED_TIME DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(256),
    LAST_UPDATED_TIME DATETIME,
    CONSTRAINT RULESET_CONSTRAINT UNIQUE (NAME, ORGANIZATION),
    PRIMARY KEY (RULESET_ID)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_RULESET_RULE (
    RULESET_RULE_ID VARCHAR(36) NOT NULL,
    RULESET_ID VARCHAR(36) NOT NULL,
    RULE_NAME VARCHAR(256) NOT NULL,
    RULE_MESSAGE VARCHAR(1024),
    RULE_DESCRIPTION VARCHAR(1024),
    SEVERITY VARCHAR(32) NOT NULL,
    RULE_CONTENT BLOB NOT NULL,
    FOREIGN KEY (RULESET_ID) REFERENCES GOV_RULESET(RULESET_ID),
    CONSTRAINT RULESET_RULE_CONSTRAINT UNIQUE (RULESET_ID, RULE_NAME),
    PRIMARY KEY (RULESET_RULE_ID)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_POLICY (
    POLICY_ID VARCHAR(36) NOT NULL,
    NAME VARCHAR(256) NOT NULL,
    DESCRIPTION VARCHAR(1024),
    ORGANIZATION VARCHAR(128) NOT NULL,
    CREATED_BY VARCHAR(256),
    CREATED_TIME DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(256),
    LAST_UPDATED_TIME DATETIME,
    IS_GLOBAL INT DEFAULT 0, -- 1 - Global, 0 - Not Global
    CONSTRAINT POLICY_CONSTRAINT UNIQUE (NAME, ORGANIZATION),
    PRIMARY KEY (POLICY_ID)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_POLICY_LABEL (
    POLICY_ID VARCHAR(36) NOT NULL,
    LABEL VARCHAR(128) NOT NULL,
    FOREIGN KEY (POLICY_ID) REFERENCES GOV_POLICY(POLICY_ID),
    PRIMARY KEY (POLICY_ID, LABEL)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_POLICY_GOVERNABLE_STATE (
    POLICY_ID VARCHAR(36) NOT NULL,
    STATE VARCHAR(128) NOT NULL,
    FOREIGN KEY (POLICY_ID) REFERENCES GOV_POLICY(POLICY_ID),
    PRIMARY KEY (POLICY_ID, STATE)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_POLICY_ACTION (
    POLICY_ID VARCHAR(36) NOT NULL,
    STATE VARCHAR(128) NOT NULL,
    SEVERITY VARCHAR(32) NOT NULL,
    TYPE VARCHAR(32) NOT NULL,
    FOREIGN KEY (POLICY_ID, STATE) REFERENCES GOV_POLICY_GOVERNABLE_STATE(POLICY_ID, STATE),
    PRIMARY KEY (POLICY_ID, STATE, SEVERITY, TYPE)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_POLICY_RULESET (
    POLICY_ID VARCHAR(36) NOT NULL,
    RULESET_ID VARCHAR(36) NOT NULL,
    FOREIGN KEY (POLICY_ID) REFERENCES GOV_POLICY(POLICY_ID),
    FOREIGN KEY (RULESET_ID) REFERENCES GOV_RULESET(RULESET_ID),
    PRIMARY KEY (POLICY_ID, RULESET_ID)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_EVALUATION_REQUEST (
    REQUEST_ID VARCHAR(45) NOT NULL,
    ARTIFACT_ID VARCHAR(45) NOT NULL,
    ARTIFACT_TYPE VARCHAR(45) NOT NULL,
    POLICY_ID VARCHAR(45) NOT NULL, -- Separate to new table
    ORGANIZATION VARCHAR(100) NOT NULL,
    EVALUATION_STATUS VARCHAR(45) NOT NULL DEFAULT 'PENDING', -- Write lock to change it to in progress
    REQUESTED_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- In progress time stamp
    CONSTRAINT EVALUATION_TRACKER_CONSTRAINT UNIQUE (ARTIFACT_ID, ARTIFACT_TYPE, POLICY_ID, EVALUATION_STATUS, ORGANIZATION),
    FOREIGN KEY (POLICY_ID) REFERENCES GOV_POLICY(POLICY_ID),
    PRIMARY KEY (REQUEST_ID)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_EVALUATION_RESULT ( -- Break entry to three tables
    RESULT_ID VARCHAR(45) NOT NULL,
    ARTIFACT_ID VARCHAR(45) NOT NULL,
    ARTIFACT_TYPE VARCHAR(45) NOT NULL,
    POLICY_ID VARCHAR(45) NOT NULL,
    RULESET_ID VARCHAR(45) NOT NULL,
    ORGANIZATION VARCHAR(100) NOT NULL,
    EVALUATION_RESULT INT NOT NULL DEFAULT 0, -- 1 - Success, 0 - Failed
    EVALUATED_TIMESTAMP DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT EVALUATION_RESULT_CONSTRAINT UNIQUE (ARTIFACT_ID, ARTIFACT_TYPE, POLICY_ID, RULESET_ID),
    FOREIGN KEY (POLICY_ID) REFERENCES GOV_POLICY(POLICY_ID),
    FOREIGN KEY (RULESET_ID) REFERENCES GOV_RULESET(RULESET_ID),
    PRIMARY KEY (RESULT_ID)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS GOV_RULE_VIOLATION ( -- Break entry to three tables
    VIOLATION_ID VARCHAR(45) NOT NULL,
    RESULT_ID VARCHAR(45) NOT NULL,
    RULESET_ID VARCHAR(45) NOT NULL,
    RULE_NAME VARCHAR(200) NOT NULL,
    VIOLATED_PATH VARCHAR(1024) NOT NULL,
    CONSTRAINT RULE_VIOLATION_CONSTRAINT UNIQUE (RESULT_ID, RULE_NAME, VIOLATED_PATH),
    FOREIGN KEY (RESULT_ID) REFERENCES GOV_EVALUATION_RESULT(RESULT_ID),
    FOREIGN KEY (RULESET_ID, RULE_NAME) REFERENCES GOV_RULESET_RULE(RULESET_ID, RULE_NAME),
    PRIMARY KEY (VIOLATION_ID)
) ENGINE=INNODB;


