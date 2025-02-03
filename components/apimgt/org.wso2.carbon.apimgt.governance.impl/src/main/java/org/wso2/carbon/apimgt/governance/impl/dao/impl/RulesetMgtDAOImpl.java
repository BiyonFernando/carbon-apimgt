/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.governance.impl.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.governance.api.ValidationEngine;
import org.wso2.carbon.apimgt.governance.api.error.GovernanceException;
import org.wso2.carbon.apimgt.governance.api.error.GovernanceExceptionCodes;
import org.wso2.carbon.apimgt.governance.api.model.ExtendedArtifactType;
import org.wso2.carbon.apimgt.governance.api.model.Rule;
import org.wso2.carbon.apimgt.governance.api.model.RuleCategory;
import org.wso2.carbon.apimgt.governance.api.model.RuleType;
import org.wso2.carbon.apimgt.governance.api.model.Ruleset;
import org.wso2.carbon.apimgt.governance.api.model.RulesetInfo;
import org.wso2.carbon.apimgt.governance.api.model.RulesetList;
import org.wso2.carbon.apimgt.governance.api.model.Severity;
import org.wso2.carbon.apimgt.governance.impl.GovernanceConstants;
import org.wso2.carbon.apimgt.governance.impl.dao.RulesetMgtDAO;
import org.wso2.carbon.apimgt.governance.impl.dao.constants.SQLConstants;
import org.wso2.carbon.apimgt.governance.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.governance.impl.util.GovernanceDBUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the RulesetMgtDAO interface.
 */
public class RulesetMgtDAOImpl implements RulesetMgtDAO {

    private static final Log log = LogFactory.getLog(RulesetMgtDAOImpl.class);

    private RulesetMgtDAOImpl() {

    }

    private static class SingletonHelper {

        private static final RulesetMgtDAO INSTANCE = new RulesetMgtDAOImpl();
    }

    /**
     * Get the instance of the RulesetMgtDAOImpl
     *
     * @return RulesetMgtDAOImpl instance
     */
    public static RulesetMgtDAO getInstance() {

        return SingletonHelper.INSTANCE;
    }

    /**
     * Create a new Governance Ruleset
     *
     * @param ruleset      Ruleset object
     * @param organization Organization
     * @return RulesetInfo Created object
     * @throws GovernanceException If an error occurs while creating the ruleset
     */
    @Override
    public RulesetInfo createRuleset(Ruleset ruleset, String organization) throws GovernanceException {

        String rulesetContent = ruleset.getRulesetContent();

        String sqlQuery = SQLConstants.CREATE_RULESET;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery);
             InputStream rulesetContentStream = new ByteArrayInputStream
                     (rulesetContent.getBytes(StandardCharsets.UTF_8))) {
            try {
                connection.setAutoCommit(false);
                prepStmt.setString(1, ruleset.getId());
                prepStmt.setString(2, ruleset.getName());
                prepStmt.setString(3, ruleset.getDescription());
                prepStmt.setBlob(4, rulesetContentStream);
                prepStmt.setString(5, String.valueOf(ruleset.getRuleCategory()));
                prepStmt.setString(6, String.valueOf(ruleset.getRuleType()));
                prepStmt.setString(7, String.valueOf(ruleset.getArtifactType()));
                prepStmt.setString(8, ruleset.getDocumentationLink());
                prepStmt.setString(9, ruleset.getProvider());
                prepStmt.setString(10, organization);
                prepStmt.setString(11, ruleset.getCreatedBy());
                prepStmt.execute();

                ValidationEngine validationEngine = ServiceReferenceHolder.getInstance()
                        .getValidationEngineService().getValidationEngine();
                List<Rule> rules = validationEngine.extractRulesFromRuleset(ruleset);
                if (rules.size() > 0) {
                    addRules(ruleset.getId(), rules, connection);
                } else {
                    throw new GovernanceException(
                            GovernanceExceptionCodes.INVALID_RULESET_CONTENT, ruleset.getName());
                }
                connection.commit();
            } catch (SQLException | GovernanceException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException | IOException e) {
            if (e instanceof SQLIntegrityConstraintViolationException) {
                if (getRulesetByName(ruleset.getName(), organization) != null) {
                    throw new GovernanceException(GovernanceExceptionCodes.RULESET_ALREADY_EXIST, ruleset.getName(),
                            organization);
                }
            }
            throw new GovernanceException(GovernanceExceptionCodes.RULESET_CREATION_FAILED, e,
                    ruleset.getName(), organization
            );
        }
        return getRulesetById(ruleset.getId()); // to return RulesetInfo object
    }

    /**
     * Update a Governance Ruleset
     *
     * @param rulesetId Ruleset ID
     * @param ruleset   Ruleset object
     * @return RulesetInfo Created object
     * @throws GovernanceException If an error occurs while updating the ruleset
     */
    @Override
    public RulesetInfo updateRuleset(String rulesetId, Ruleset ruleset)
            throws GovernanceException {

        String rulesetContent = ruleset.getRulesetContent();

        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(SQLConstants.UPDATE_RULESET);
             InputStream rulesetContentStream = new ByteArrayInputStream(rulesetContent
                     .getBytes(StandardCharsets.UTF_8))) {
            try {
                connection.setAutoCommit(false);
                prepStmt.setString(1, ruleset.getName());
                prepStmt.setString(2, ruleset.getDescription());
                prepStmt.setBlob(3, rulesetContentStream);
                prepStmt.setString(4, String.valueOf(ruleset.getRuleCategory()));
                prepStmt.setString(5, String.valueOf(ruleset.getRuleType()));
                prepStmt.setString(6, String.valueOf(ruleset.getArtifactType()));
                prepStmt.setString(7, ruleset.getDocumentationLink());
                prepStmt.setString(8, ruleset.getProvider());
                prepStmt.setString(9, ruleset.getUpdatedBy());
                prepStmt.setString(10, rulesetId);
                int rowsAffected = prepStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new GovernanceException(GovernanceExceptionCodes.RULESET_NOT_FOUND,
                            rulesetId);
                }
                // Delete existing rules and rule evaluation results related to this ruleset
                deleteRules(rulesetId, connection);
                deleteRuleViolationsForRuleset(rulesetId, connection);
                deleteEvaluationResultsForRuleset(rulesetId, connection);

                // Insert updated rules to the database
                ValidationEngine validationEngine = ServiceReferenceHolder.getInstance()
                        .getValidationEngineService().getValidationEngine();
                List<Rule> rules = validationEngine.extractRulesFromRuleset(ruleset);
                if (rules.size() > 0) {
                    addRules(ruleset.getId(), rules, connection);
                } else {
                    throw new GovernanceException(
                            GovernanceExceptionCodes.INVALID_RULESET_CONTENT, ruleset.getName());
                }

                connection.commit();
            } catch (SQLException | GovernanceException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException | IOException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_UPDATING_RULESET, e, rulesetId);
        }
        return getRulesetById(rulesetId); // to return all info of the updated ruleset
    }

    /**
     * Add rules in a ruleset to DB
     *
     * @param rulesetId  Ruleset ID
     * @param rules      List of rules
     * @param connection Database connection
     * @throws GovernanceException If an error occurs while adding the rules
     */
    private void addRules(String rulesetId, List<Rule> rules, Connection connection)
            throws GovernanceException {
        String sqlQuery = SQLConstants.ADD_RULES;
        try (PreparedStatement prepStmt = connection.prepareStatement(sqlQuery);) {
            for (Rule rule : rules) {
                prepStmt.setString(1, rule.getId());
                prepStmt.setString(2, rulesetId);
                prepStmt.setString(3, rule.getName());
                prepStmt.setString(4, rule.getMessageOnFailure());
                prepStmt.setString(5, rule.getDescription());
                prepStmt.setString(6, String.valueOf(rule.getSeverity()));
                prepStmt.setBlob(7, new ByteArrayInputStream(rule.getContent()
                        .getBytes(Charset.defaultCharset())));
                prepStmt.addBatch();
            }
            prepStmt.executeBatch();
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_INSERTING_RULES, e, rulesetId);
        }

    }

    /**
     * Delete a Governance Ruleset
     *
     * @param rulesetId Ruleset ID
     * @throws GovernanceException If an error occurs while deleting the ruleset
     */
    @Override
    public void deleteRuleset(String rulesetId) throws GovernanceException {

        try (Connection connection = GovernanceDBUtil.getConnection()) {
            try {
                connection.setAutoCommit(false);

                // Delete rules associated with the ruleset, here we don't need to delete results as prior to
                // deleting ruleset, we force the update of policies which will delete the results
                deleteRules(rulesetId, connection);

                try (PreparedStatement prepStmt = connection.prepareStatement(SQLConstants.DELETE_RULESET)) {
                    prepStmt.setString(1, rulesetId);
                    int rowsAffected = prepStmt.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new GovernanceException(GovernanceExceptionCodes.RULESET_NOT_FOUND,
                                rulesetId);
                    }
                }

                connection.commit();
            } catch (SQLException | GovernanceException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_DELETING_RULESET,
                    e, rulesetId);
        }
    }

    /**
     * Delete rules related to a ruleset
     *
     * @param rulesetId  Ruleset ID
     * @param connection Database connection
     * @throws GovernanceException If an error occurs while deleting the rules
     */
    private void deleteRules(String rulesetId, Connection connection) throws GovernanceException {
        try (PreparedStatement prepStmt = connection.prepareStatement(SQLConstants.DELETE_RULES)) {
            prepStmt.setString(1, rulesetId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_DELETING_RULES, e, rulesetId);
        }
    }

    /**
     * Check whether a ruleset is associated with policies
     *
     * @param rulesetId  Ruleset ID
     * @param connection Database connection
     * @throws GovernanceException If an error occurs while checking the association
     */
    private void deleteRuleViolationsForRuleset(String rulesetId, Connection connection)
            throws GovernanceException {
        try (PreparedStatement prepStmt = connection.prepareStatement(SQLConstants
                .DELETE_RULE_VIOLATIONS_BY_RULESET)) {
            prepStmt.setString(1, rulesetId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes
                    .ERROR_WHILE_DELETING_RULE_VIOLATIONS_BY_RESULT_ID,
                    e, rulesetId);
        }
    }

    /**
     * Delete compliance evaluation results related to a ruleset
     *
     * @param rulesetId  Ruleset ID
     * @param connection Database connection
     * @throws GovernanceException If an error occurs while deleting the compliance
     *                             evaluation results
     */
    private void deleteEvaluationResultsForRuleset(String rulesetId, Connection connection)
            throws GovernanceException {
        try (PreparedStatement prepStmt = connection.
                prepareStatement(SQLConstants.DELETE_GOV_COMPLIANCE_EVALUATION_RESULT_BY_RULESET)) {
            prepStmt.setString(1, rulesetId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes
                    .ERROR_WHILE_DELETING_EVALUATION_RESULTS_FOR_RULESET,
                    e, rulesetId);
        }
    }

    /**
     * Retrieves rulesets in the organization.
     *
     * @param organization Organization whose rulesets are to be retrieved
     * @return a list of rulesets associated with the organization
     * @throws GovernanceException if there is an error retrieving the rulesets
     */
    @Override
    public RulesetList getRulesets(String organization) throws GovernanceException {
        RulesetList rulesetList = new RulesetList();
        List<RulesetInfo> rulesetInfoList = new ArrayList<>();
        String sqlQuery = SQLConstants.GET_RULESETS;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery)) {
            prepStmt.setString(1, organization);
            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    rulesetInfoList.add(getRulesetInfoFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_RULESETS,
                    e, organization);
        }
        rulesetList.setCount(rulesetInfoList.size());
        rulesetList.setRulesetList(rulesetInfoList);
        return rulesetList;
    }

    /**
     * Retrieves a ruleset by name.
     *
     * @param name         Name of the ruleset
     * @param organization Organization whose ruleset is to be retrieved
     * @return the ruleset with the given name
     * @throws GovernanceException if there is an error retrieving the ruleset
     */
    @Override
    public RulesetInfo getRulesetByName(String name, String organization) throws GovernanceException {
        String sqlQuery = SQLConstants.GET_RULESET_BY_NAME;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery)) {
            prepStmt.setString(1, name);
            prepStmt.setString(2, organization);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    return getRulesetInfoFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_RULESET_BY_NAME,
                    e, organization);
        }
        return null;
    }

    /**
     * Retrieves a ruleset by ID.
     *
     * @param rulesetId Ruleset ID of the ruleset
     * @return the ruleset with the given ID
     * @throws GovernanceException if there is an error retrieving the ruleset
     */
    @Override
    public RulesetInfo getRulesetById(String rulesetId) throws GovernanceException {
        String sqlQuery = SQLConstants.GET_RULESETS_BY_ID;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery)) {
            prepStmt.setString(1, rulesetId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    return getRulesetInfoFromResultSet(rs);
                } else {
                    throw new GovernanceException(GovernanceExceptionCodes.RULESET_NOT_FOUND,
                            rulesetId);
                }
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_RULESET_BY_ID,
                    e);
        }
    }

    /**
     * Search for Governance Rulesets based on the search criteria
     *
     * @param searchCriteria Search attributes
     * @param organization   Organization
     * @return List of RulesetInfo objects
     * @throws GovernanceException If an error occurs while searching for rulesets
     */
    @Override
    public RulesetList searchRulesets(Map<String, String> searchCriteria, String organization)
            throws GovernanceException {
        RulesetList rulesetList = new RulesetList();
        List<RulesetInfo> rulesetInfoList = new ArrayList<>();

        String sqlQuery = SQLConstants.SEARCH_RULESETS;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery)) {
            prepStmt.setString(1, organization);
            prepStmt.setString(2, "%" + searchCriteria
                    .getOrDefault(GovernanceConstants.RulesetSearchAttributes.NAME, "") + "%");
            prepStmt.setString(3, "%" + searchCriteria
                    .getOrDefault(GovernanceConstants.RulesetSearchAttributes.RULE_TYPE, "") + "%");
            prepStmt.setString(4, "%" + searchCriteria
                    .getOrDefault(GovernanceConstants.RulesetSearchAttributes.ARTIFACT_TYPE, "") + "%");
            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    rulesetInfoList.add(getRulesetInfoFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_SEARCHING_RULESETS,
                    e, organization);
        }
        rulesetList.setCount(rulesetInfoList.size());
        rulesetList.setRulesetList(rulesetInfoList);
        return rulesetList;
    }

    /**
     * Retrieves rulesetInfo object from the result set
     *
     * @param rs ResultSet
     * @return RulesetInfo object
     * @throws SQLException If an error occurs while retrieving the ruleset
     */
    private RulesetInfo getRulesetInfoFromResultSet(ResultSet rs) throws SQLException {
        RulesetInfo rulesetInfo = new RulesetInfo();
        rulesetInfo.setId(rs.getString("RULESET_ID"));
        rulesetInfo.setName(rs.getString("NAME"));
        rulesetInfo.setDescription(rs.getString("DESCRIPTION"));
        rulesetInfo.setRuleCategory(RuleCategory.fromString(
                rs.getString("RULE_CATEGORY")));
        rulesetInfo.setRuleType(RuleType.fromString(rs.getString("RULE_TYPE")));
        rulesetInfo.setArtifactType(ExtendedArtifactType.fromString(
                rs.getString("ARTIFACT_TYPE")));
        rulesetInfo.setDocumentationLink(rs.getString("DOCUMENTATION_LINK"));
        rulesetInfo.setProvider(rs.getString("PROVIDER"));
        rulesetInfo.setCreatedBy(rs.getString("CREATED_BY"));
        rulesetInfo.setCreatedTime(rs.getString("CREATED_TIME"));
        rulesetInfo.setUpdatedBy(rs.getString("UPDATED_BY"));
        rulesetInfo.setUpdatedTime(rs.getString("LAST_UPDATED_TIME"));
        return rulesetInfo;
    }

    /**
     * Get the content of a Governance Ruleset
     *
     * @param rulesetId Ruleset ID
     * @return String Content of the ruleset
     * @throws GovernanceException If an error occurs while getting the ruleset content
     */
    @Override
    public String getRulesetContent(String rulesetId) throws GovernanceException {
        String rulesetContent = null;
        String sqlQuery = SQLConstants.GET_RULESET_CONTENT;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery);) {
            prepStmt.setString(1, rulesetId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    try (InputStream inputStream = rs.getBinaryStream("RULESET_CONTENT")) {
                        if (inputStream != null) {
                            rulesetContent = GovernanceDBUtil.getStringFromInputStream(inputStream);
                        }
                    }
                    return rulesetContent;
                } else {
                    throw new GovernanceException(GovernanceExceptionCodes.RULESET_NOT_FOUND, rulesetId);
                }
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_RULESET_BY_ID,
                    e);
        } catch (IOException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_RULESET_CONTENT, e,
                    rulesetId);
        }
    }

    /**
     * Get the associated policies for a Ruleset
     *
     * @param rulesetId Ruleset ID
     * @return List of associated policies
     */
    @Override
    public List<String> getAssociatedPoliciesForRuleset(String rulesetId) throws GovernanceException {
        List<String> policyIds = new ArrayList<>();
        String sqlQuery = SQLConstants.GET_POLICIES_FOR_RULESET;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery)) {
            prepStmt.setString(1, rulesetId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    policyIds.add(rs.getString("POLICY_ID"));
                }
            }
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_ASSOCIATED_POLICIES,
                    e, rulesetId);
        }
        return policyIds;
    }

    /**
     * Get the rules of a Ruleset (without the content)
     *
     * @param rulesetId Ruleset ID
     * @return List of rules
     */
    @Override
    public List<Rule> getRulesByRulesetId(String rulesetId) throws GovernanceException {
        List<Rule> rules = new ArrayList<>();
        String sqlQuery = SQLConstants.GET_RULES_WITHOUT_CONTENT;
        try (Connection connection = GovernanceDBUtil.getConnection();
             PreparedStatement prepStmt = connection.prepareStatement(sqlQuery)) {
            prepStmt.setString(1, rulesetId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    Rule rule = new Rule();
                    rule.setId(rs.getString("RULESET_RULE_ID"));
                    rule.setName(rs.getString("RULE_NAME"));
                    rule.setDescription(rs.getString("RULE_DESCRIPTION"));
                    rule.setMessageOnFailure(rs.getString("RULE_MESSAGE"));
                    rule.setSeverity(Severity.fromString(rs.getString("SEVERITY")));
                    rules.add(rule);
                }
            }
            return rules;
        } catch (SQLException e) {
            throw new GovernanceException(GovernanceExceptionCodes.ERROR_WHILE_RETRIEVING_RULES_BY_RULESET_ID
                    , e, rulesetId);
        }
    }
}

