/******************************************************************************
 * Copyright © 2013-2016 The kpl Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * kpl software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package kpl.http.monetarysystem;

import kpl.BlockchainTest;
import kpl.Constants;
import kpl.CurrencyType;
import kpl.http.APICall;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyIssuance extends BlockchainTest {

    @Test
    public void issueCurrency() {
        APICall apiCall = new Builder().build();
        issueCurrencyApi(apiCall);
    }

    @Test
    public void issueMultipleCurrencies() {
        APICall apiCall = new Builder().naming("axc", "AXC", "Currency A").build();
        issueCurrencyApi(apiCall);
        apiCall = new Builder().naming("bXbx", "BXBX", "Currency B").feeNQT(1000 * Constants.ONE_kpl).build();
        issueCurrencyApi(apiCall);
        apiCall = new Builder().naming("ccXcc", "CCCXC", "Currency C").feeNQT(40 * Constants.ONE_kpl).build();
        issueCurrencyApi(apiCall);
        apiCall = new APICall.Builder("getCurrency").param("code", "BXBX").build();
        JSONObject response = apiCall.invoke();
        Assert.assertEquals("bXbx", response.get("name"));
    }

    static String issueCurrencyApi(APICall apiCall) {
        JSONObject issueCurrencyResponse = apiCall.invoke();
        String currencyId = (String) issueCurrencyResponse.get("transaction");
        generateBlock();

        apiCall = new APICall.Builder("getCurrency").param("currency", currencyId).build();
        JSONObject getCurrencyResponse = apiCall.invoke();
        Assert.assertEquals(currencyId, getCurrencyResponse.get("currency"));
        return currencyId;
    }

    public static class Builder extends APICall.Builder {

        public Builder() {
            super("issueCurrency");
            secretPhrase(ALICE.getSecretPhrase());
            feeNQT(0l);
            //feeNQT(25000 * Constants.ONE_kpl);
            param("name", "Test1");
            param("code", "TSXXX");
            param("description", "Test Currency 1");
            param("type", CurrencyType.EXCHANGEABLE.getCode());
            param("maxSupply", 100000);
            param("initialSupply", 100000);
            param("issuanceHeight", 0);
            param("algorithm", (byte)0);
        }

        public Builder naming(String name, String code, String description) {
            param("name", name);
            param("code", code).
            param("description", description);
            return this;
        }

        public Builder type(int type) {
            param("type", type);
            return this;
        }

        public Builder maxSupply(long maxSupply) {
            param("maxSupply", maxSupply);
            return this;
        }

        public Builder reserveSupply(long reserveSupply) {
            param("reserveSupply", reserveSupply);
            return this;
        }

        public Builder initialSupply(long initialSupply) {
            param("initialSupply", initialSupply);
            return this;
        }

        public Builder issuanceHeight(int issuanceHeight) {
            param("issuanceHeight", issuanceHeight);
            return this;
        }

        public Builder minReservePerUnitNQT(long minReservePerUnitNQT) {
            param("minReservePerUnitNQT", minReservePerUnitNQT);
            return this;
        }

        public Builder minting(byte minDifficulty, byte maxDifficulty, byte algorithm) {
            param("minDifficulty", minDifficulty);
            param("maxDifficulty", maxDifficulty);
            param("algorithm", algorithm);
            return this;
        }

    }
}
