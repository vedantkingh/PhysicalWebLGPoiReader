package gsoc.google.com.physicalweblgpoireader.utils;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import gsoc.google.com.physicalweblgpoireader.PW.NearbyBeaconsFragment;


public class FragmentStackManager {

    private static FragmentStackManager mInstance = null;
    private static Context mContext = null;
    private static String TAG = "FragmentStackManager.class";
    private FragmentManager fragmentManager;

    public FragmentStackManager(){
        try{
            final AppCompatActivity activity = (AppCompatActivity) mContext;
            fragmentManager = activity.getSupportFragmentManager();
        } catch (ClassCastException e) {
            Log.d(TAG, "Can't get the fragment manager with this");
        }
    }

    public static FragmentStackManager getInstance(Context context){
        if(mInstance==null || !mContext.equals(context)){
            mContext = context;
            mInstance =  new FragmentStackManager();
        }
        return mInstance;
    }

    /**
     * Load new fragment to a resource given as parameter
     */
    public void loadFragment(Fragment toLoad, int backFrame) {
        String fragmentName = toLoad.getClass().getName();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(backFrame, toLoad, fragmentName);
        ft.addToBackStack(fragmentName);
        ft.commit();
    }

    /**
     * Pop 1 fragment from de backstack and returns false if you can't pop more items.
     */
    public boolean popBackStatFragment(){
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStack();
            fragmentManager.beginTransaction().commit();
            return true;
        }
        return false;
    }


    /**
     * Pop until last fragment.
     */
    public void resetBackStack(Fragment fragment){
        fragmentManager.popBackStack(fragment.getClass().getName(), 0);
    }
}
