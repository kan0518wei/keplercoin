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

package kpl.http;

import kpl.Account;
import kpl.kplException;
import kpl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyAccounts extends APIServlet.APIRequestHandler {

    static final GetCurrencyAccounts instance = new GetCurrencyAccounts();

    private GetCurrencyAccounts() {
        super(new APITag[] {APITag.MS}, "currency", "height", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws kplException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int height = ParameterParser.getHeight(req);

        JSONArray accountCurrencies = new JSONArray();
        try (DbIterator<Account.AccountCurrency> iterator = Account.getCurrencyAccounts(currencyId, height, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Account.AccountCurrency accountCurrency = iterator.next();
                accountCurrencies.add(JSONData.accountCurrency(accountCurrency, true, false));
            }
        }

        JSONObject response = new JSONObject();
        response.put("accountCurrencies", accountCurrencies);
        return response;

    }

}
