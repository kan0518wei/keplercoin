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
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyAccountCount extends APIServlet.APIRequestHandler {

    static final GetCurrencyAccountCount instance = new GetCurrencyAccountCount();

    private GetCurrencyAccountCount() {
        super(new APITag[] {APITag.MS}, "currency", "height");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws kplException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", true);
        int height = ParameterParser.getHeight(req);

        JSONObject response = new JSONObject();
        response.put("numberOfAccounts", Account.getCurrencyAccountCount(currencyId, height));
        return response;

    }

}
