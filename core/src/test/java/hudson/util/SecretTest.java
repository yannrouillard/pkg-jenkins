/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.util;

import jenkins.model.Jenkins;
import junit.framework.TestCase;

import java.security.SecureRandom;

import hudson.Util;

/**
 * @author Kohsuke Kawaguchi
 */
public class SecretTest extends TestCase {
    @Override protected void setUp() throws Exception {
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[32];
        sr.nextBytes(random);
        Secret.SECRET = Util.toHexString(random);

    }

    @Override protected void tearDown() throws Exception {
        Secret.SECRET = null;
    }

    public void testEncrypt() {
        Secret secret = Secret.fromString("abc");
        assertEquals("abc",secret.getPlainText());

        // make sure we got some encryption going
        System.out.println(secret.getEncryptedValue());
        assertTrue(!"abc".equals(secret.getEncryptedValue()));

        // can we round trip?
        assertEquals(secret,Secret.fromString(secret.getEncryptedValue()));
    }

    public void testDecrypt() {
        assertEquals("abc",Secret.toString(Secret.fromString("abc")));
    }

    public void testSerialization() {
        Secret s = Secret.fromString("Mr.Hudson");
        String xml = Jenkins.XSTREAM.toXML(s);
        assertTrue(xml, !xml.contains(s.getPlainText()));
        assertTrue(xml, xml.contains(s.getEncryptedValue()));
        Object o = Jenkins.XSTREAM.fromXML(xml);
        assertEquals(xml, o, s);
    }

    public static class Foo {
        Secret password;
    }

    /**
     * Makes sure the serialization form is backward compatible with String.
     */
    public void testCompatibilityFromString() {
        String tagName = Foo.class.getName().replace("$","_-");
        String xml = "<"+tagName+"><password>secret</password></"+tagName+">";
        Foo foo = new Foo();
        Jenkins.XSTREAM.fromXML(xml, foo);
        assertEquals("secret",Secret.toString(foo.password));
    }
}
