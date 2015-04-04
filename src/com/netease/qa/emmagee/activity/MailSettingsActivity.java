/*
 * Copyright (c) 2012-2013 NetEase, Inc. and other contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.netease.qa.emmagee.activity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.qa.emmagee.R;
import com.netease.qa.emmagee.utils.EncryptData;
import com.netease.qa.emmagee.utils.Settings;

/**
 * Mail Setting Page of Emmagee
 * 
 * @author andrewleo
 */
public class MailSettingsActivity extends Activity {

	private static final String LOG_TAG = "Emmagee-" + MailSettingsActivity.class.getSimpleName();
	private static final String BLANK_STRING = "";

	private EditText edtRecipients; //接收收件人的组件
	private EditText edtSender;//接收发件人的组件
	private EditText edtPassword;//接搜密码的组件
	private EditText edtSmtp;//接收服务器的组件
	private String sender;
	private String prePassword, curPassword;
	private String recipients, smtp;
	private String[] receivers;
	private TextView title;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mail_settings);

		final EncryptData des = new EncryptData("emmagee");

		/*find view*/
		edtSender = (EditText) findViewById(R.id.sender); //获取用来输入发件人的组件
		edtPassword = (EditText) findViewById(R.id.password);//获取用来输入密码的组件
		edtRecipients = (EditText) findViewById(R.id.recipients);//获取用来输入收件人的组件
		edtSmtp = (EditText) findViewById(R.id.smtp);//获取用来输入服务器名称的组件
		title = (TextView) findViewById(R.id.nb_title);//获取title文字的组件
		LinearLayout layGoBack = (LinearLayout) findViewById(R.id.lay_go_back);//获取图标的组件
		LinearLayout layBtnSet = (LinearLayout) findViewById(R.id.lay_btn_set);//获取设置图标的组件

		title.setText(R.string.mail_settings);//

		/*SharedPreference是Android提供的一种轻量级的数据存储方式，主要用来存储一些简单的配置信息
		 *  通过SharedPreferences对象的键key可以获取到对应key的键值
		 *  数据的存入是通过SharedPreferences对象的编辑器对象Editor来实现的
		 * */
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);//获取一个shared的对象
		sender = preferences.getString(Settings.KEY_SENDER, BLANK_STRING);
		prePassword = preferences.getString(Settings.KEY_PASSWORD, BLANK_STRING);
		recipients = preferences.getString(Settings.KEY_RECIPIENTS, BLANK_STRING);
		smtp = preferences.getString(Settings.KEY_SMTP, BLANK_STRING);

		edtRecipients.setText(recipients);
		edtSender.setText(sender);
		edtPassword.setText(prePassword);
		edtSmtp.setText(smtp);

		/*设置返回键的监听事件*/
		layGoBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MailSettingsActivity.this.finish();
			}
		});
		/*设置保存键的监听事件
		 **
		 * */
		layBtnSet.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sender = edtSender.getText().toString().trim();
				if (!BLANK_STRING.equals(sender) && !checkMailFormat(sender)) {
					Toast.makeText(MailSettingsActivity.this, getString(R.string.sender_mail_toast) + getString(R.string.format_incorrect_format),
							Toast.LENGTH_LONG).show();
					return;
				}
				recipients = edtRecipients.getText().toString().trim();
				receivers = recipients.split("\\s+");
				for (int i = 0; i < receivers.length; i++) {
					if (!BLANK_STRING.equals(receivers[i]) && !checkMailFormat(receivers[i])) {
						Toast.makeText(MailSettingsActivity.this,
								getString(R.string.receiver_mail_toast) + "[" + receivers[i] + "]" + getString(R.string.format_incorrect_format),
								Toast.LENGTH_LONG).show();
						return;
					}
				}
				curPassword = edtPassword.getText().toString().trim();
				smtp = edtSmtp.getText().toString().trim();
				if (checkMailConfig(sender, recipients, smtp, curPassword) == -1) {
					Toast.makeText(MailSettingsActivity.this, getString(R.string.info_incomplete_toast), Toast.LENGTH_LONG).show();
					return;
				}
				SharedPreferences preferences = Settings.getDefaultSharedPreferences(getApplicationContext());
				Editor editor = preferences.edit();
				editor.putString(Settings.KEY_SENDER, sender);

				try {
					editor.putString(Settings.KEY_PASSWORD, curPassword.equals(prePassword) ? curPassword : des.encrypt(curPassword));
				} catch (Exception e) {
					editor.putString(Settings.KEY_PASSWORD, curPassword);
				}
				editor.putString(Settings.KEY_RECIPIENTS, recipients);
				editor.putString(Settings.KEY_SMTP, smtp);
				editor.commit();
				Toast.makeText(MailSettingsActivity.this, getString(R.string.save_success_toast), Toast.LENGTH_LONG).show();
				Intent intent = new Intent();
				setResult(Activity.RESULT_FIRST_USER, intent);
				MailSettingsActivity.this.finish();
			}
		});
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * check if mail configurations are available
	 * 
	 * @param sender
	 * @param recipients
	 * @param smtp
	 * @param curPassword
	 * @return true: valid configurations
	 * 		   
	 */
	private int checkMailConfig(String sender, String recipients, String smtp, String curPassword) {
		if (!BLANK_STRING.equals(curPassword) && !BLANK_STRING.equals(sender) && !BLANK_STRING.equals(recipients) && !BLANK_STRING.equals(smtp)) {
			return 1;
		} else if (BLANK_STRING.equals(curPassword) && BLANK_STRING.equals(sender) && BLANK_STRING.equals(recipients) && BLANK_STRING.equals(smtp)) {
			return 0;
		} else
			return -1;
	}

	/**
	 * check mail format
	 * 
	 * @return true: valid email address
	 */
	private boolean checkMailFormat(String mail) {
		String strPattern = "^[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*" + "[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
		Pattern p = Pattern.compile(strPattern);
		Matcher m = p.matcher(mail);
		return m.matches();
	}
}
