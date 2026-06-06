package com.posmix.mixtuvgag.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "micropos_session";
    private static final String KEY_LOGGED_IN   = "is_logged_in";
    private static final String KEY_USERNAME     = "username";
    private static final String KEY_EMP_ID       = "employee_id";
    private static final String KEY_EMP_NAME     = "employee_name";
    private static final String KEY_EMP_ROLE     = "employee_role";

    private final SharedPreferences sp;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        sp     = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    public void setLogin(boolean loggedIn, String username) {
        editor.putBoolean(KEY_LOGGED_IN, loggedIn);
        if (username != null) editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public void setLogin(boolean loggedIn, String username, int empId, String empName, int role) {
        editor.putBoolean(KEY_LOGGED_IN, loggedIn);
        editor.putString(KEY_USERNAME, username);
        editor.putInt(KEY_EMP_ID, empId);
        editor.putString(KEY_EMP_NAME, empName);
        editor.putInt(KEY_EMP_ROLE, role);
        editor.apply();
    }

    public boolean isLoggedIn()     { return sp.getBoolean(KEY_LOGGED_IN, false); }
    public String  getUsername()    { return sp.getString(KEY_USERNAME, ""); }
    public int     getEmployeeId()  { return sp.getInt(KEY_EMP_ID, 0); }
    public String  getEmployeeName(){ return sp.getString(KEY_EMP_NAME, ""); }
    public int     getEmployeeRole(){ return sp.getInt(KEY_EMP_ROLE, 0); }
    public boolean isAdmin()        { return getEmployeeRole() == 1; }

    public void logout() { editor.clear(); editor.apply(); }

    public void checkLogin() {
        if (!isLoggedIn()) {
            android.content.Intent i = new android.content.Intent(context,
                com.posmix.mixtuvgag.activities.LoginActivity.class);
            i.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
        }
    }
}
