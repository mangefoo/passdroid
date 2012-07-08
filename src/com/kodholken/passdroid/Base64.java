/*    
    This file is part of the Passdroid password management software.

    Copyright (C) 2009-2012  Magnus Eriksson <eriksson.mag@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kodholken.passdroid;

public class Base64 {
    private static final char table[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/' 
    };

    public static String encode(byte [] data) {
        String result = "";
        int i, a, b, c;

        i = 0;
        while (i + 2 < data.length) {
            a = data[i];
            b = data[i + 1];
            c = data[i + 2];

            result += table[(a >> 2) & 0x3f];
            result += table[((a << 4) & 0x30) + ((b >> 4) & 0x0f)];
            result += table[((b << 2) & 0x3c) + ((c >> 6) & 0x03)];
            result += table[c & 0x3f];

            i += 3;
        }

        switch (data.length - i) {
        case 1:
            a = data[i];
            result += table[(a >> 2) & 0x3f];
            result += table[(a << 4) & 0x30];
            result += "==";
            break;
        case 2:
            a = data[i];
            b = data[i + 1];
            result += table[(a >> 2) & 0x3f];
            result += table[((a << 4) & 0x30) + ((b >> 4) & 0x0f)];
            result += table[(b << 2) & 0x3c];
            result += "=";
            break;
        }

        return result;
    }

    public static byte getByte(char c) throws Base64Exception {
        for (int i = 0; i < table.length; i++) {
            if (table[i] == c) {
                return (byte) (i & 0xff);
            }
        }

        throw new Base64Exception();
    }

    public static byte [] decode(String data) throws Base64Exception {
        int padding = 0;
        byte [] result;

        if (data.length() % 4 != 0) {
            throw (new Base64Exception());
        }

        if (data.charAt(data.length() - 1) == '=') {
            padding = 1;
        } else {
            padding = 0;
        }

        if (data.charAt(data.length() - 2) == '=') {
            padding++;
        }

        result = new byte[data.length() / 4 * 3 - padding];

        int d = 0, e = 0;
        while (e < data.length()) {
            boolean last = (e + 4 >= data.length());

            char c1 = data.charAt(e++);
            char c2 = data.charAt(e++);
            char c3 = data.charAt(e++);
            char c4 = data.charAt(e++);

            int tmp = (getByte(c1) << 2) & 0xfc;
            int tmp2 = (getByte(c2) >> 4) & 0x03;

            result[d++] = (byte) (tmp + tmp2);
            if (last && padding > 0) {
                if (padding == 1) {
                    tmp = (getByte(c2) << 4) & 0xf0;
                    tmp2 = (getByte(c3) >> 2) & 0x0f;
                    result[d++] = (byte) (tmp + tmp2);
                }
            } else {
                tmp = (getByte(c2) << 4) & 0xf0;
                tmp2 = (getByte(c3) >> 2) & 0x0f;
                result[d++] = (byte) (tmp + tmp2);

                tmp = (getByte(c3) << 6) & 0xc0;
                tmp2 = getByte(c4) & 0x3f;
                result[d++] = (byte) (tmp + tmp2);
            }
        }

        return result;
    }

    public static void main(String [] args) {
        try {
            System.out.println("Test: " + Base64.encode(args[0].getBytes()));
            System.out.println("2: " + Base64.decode(Base64.encode(args[0].getBytes())));
            byte [] res = Base64.decode(Base64.encode(args[0].getBytes()));
            String str = new String(res, 0, res.length, "UTF8");
            System.out.println("UTF8: " + str);
            for (int i = 0; i < res.length; i++) {
                System.out.println(res[i]);
            }

            byte t [] = new byte[2];
            t[0] = -128;
            t[1] = -23;

            byte u [] = Base64.decode(Base64.encode(t));

            for (int i = 0; i < u.length; i++) {
                System.out.println(u[i]);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
