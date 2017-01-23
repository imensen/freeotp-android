import android.app.Activity;
import android.graphics.Camera;

import junit.framework.Assert;

import org.fedorahosted.freeotp.BuildConfig;
import org.fedorahosted.freeotp.MainActivity;
import org.fedorahosted.freeotp.UrlPersistence;
import org.fedorahosted.freeotp.data.Encrypter;
import org.fedorahosted.freeotp.data.TargetData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowCamera;
import org.robolectric.util.ActivityController;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")

public class MainActivityTest {
	@Test
	public void testExampleActivtiy() {
		ActivityController<MainActivity> activityController = Robolectric.buildActivity(MainActivity.class);
		Activity activity = activityController.create().visible().get();

		Assert.assertTrue(true);
	}

	@Test
	public void testUrlPersistence() {
	    UrlPersistence.addWithToast(Robolectric.application, "myotp://192.168.78.1:7480?algo=des&key=einkey&iv=einiv");

		UrlPersistence persistence = new UrlPersistence(Robolectric.application);

		TargetData result = persistence.get(0);
		Assert.assertEquals(result.getIp(), "192.168.78.1");
		Assert.assertEquals(result.getPort(), "7480");
		Assert.assertEquals(result.getAlgo(), "des");
		Assert.assertEquals(result.getKey(), "einkey");
	}

	@Test
	public void testRemoveUrlPersistence() {
		UrlPersistence.addWithToast(Robolectric.application, "myotp://192.168.78.1:7480?algo=des&key=einkey&iv=einiv");

		UrlPersistence persistence = new UrlPersistence(Robolectric.application);

		persistence.delete(0);

		UrlPersistence.addWithToast(Robolectric.application, "myotp://192.168.78.1:7480?algo=des&key=zweiterkey&iv=zweiteriv");

		TargetData result = persistence.get(0);
		Assert.assertEquals(result.getIp(), "192.168.78.1");
		Assert.assertEquals(result.getPort(), "7480");
		Assert.assertEquals(result.getAlgo(), "des");
		Assert.assertEquals(result.getKey(), "zweiterkey");
	}


	@Test
	public void testUrlDoublePersistence() {
		UrlPersistence.addWithToast(Robolectric.application, "myotp://192.168.78.1:7480?algo=des&key=einkey");
		UrlPersistence.addWithToast(Robolectric.application, "myotp://192.168.78.59:7480?algo=des&key=59340a2942b20ed4");

		UrlPersistence persistence = new UrlPersistence(Robolectric.application);
		Assert.assertEquals(persistence.length(), 1);

		TargetData result = persistence.get(0);
		Assert.assertEquals(result.getIp(), "192.168.78.59");
		Assert.assertEquals(result.getPort(), "7480");
		Assert.assertEquals(result.getAlgo(), "des");
		Assert.assertEquals(result.getKey(), "59340a2942b20ed4");
	}

	@Test
	public void testEncrypter() throws InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		Encrypter encrypter = new Encrypter("12345678".getBytes());

		byte[] result = encrypter.Encrypt("748012".getBytes());

		char[] charResult = org.apache.commons.codec.binary.Hex.encodeHex(result);

		String friendlyResult = new String(charResult);

		Assert.assertEquals("69354d1d00beadd8", friendlyResult);
	}
}