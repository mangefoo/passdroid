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

import com.kodholken.passdroid.R;

import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class AboutActivity extends TimeoutActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.about);

        TextView title = (TextView) findViewById(R.id.about_title);
        title.setText("Passdroid v" + Utils.getVersion(this));

        getString(R.string.about_text);
        ((TextView) findViewById(R.id.about_content)).setText(
                getString(R.string.about_text).replace("${VERSION}",
                        Utils.getVersion(this)));
    }
}
