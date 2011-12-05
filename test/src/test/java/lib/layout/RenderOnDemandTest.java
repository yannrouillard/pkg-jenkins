/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lib.layout;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests &lt;renderOnDemand> tag.
 *
 * @author Kohsuke Kawaguchi
 */
public class RenderOnDemandTest extends HudsonTestCase {
    /**
     * Makes sure that the behavior rules are applied to newly inserted nodes,
     * even when multiple nodes are added.
     */
    public void testBehaviour() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testBehaviour");

        p.executeJavaScript("renderOnDemand(document.getElementsBySelector('.lazy')[0])");
        // all AJAX calls complete before the above method returns

        ScriptResult r = p.executeJavaScript("var r=document.getElementsBySelector('DIV.a'); r[0].innerHTML+r[1].innerHTML+r[2].innerHTML");
        assertEquals("AlphaBravoCharlie",r.getJavaScriptResult().toString());
    }
}
