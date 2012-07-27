package org.mozilla.gecko.sync;

import android.content.SyncResult;

/**
 * There was a problem with the Sync account's credentials: bad username,
 * missing password, malformed sync key, etc.
 */
public abstract class CredentialException extends SyncException {
  private static final long serialVersionUID = 833010553314100538L;

  public void updateStats(GlobalSession globalSession, SyncResult syncResult) {
    syncResult.stats.numAuthExceptions += 1;
  }

  /**
   * No credentials at all.
   */
  public static class MissingAllCredentialsException extends CredentialException {
    private static final long serialVersionUID = 3763937096217604611L;
  }

  /**
   * Some credential is missing.
   */
  public static class MissingCredentialException extends CredentialException {
    private static final long serialVersionUID = -7543031216547596248L;

    public final String missingCredential;

    public MissingCredentialException(final String missingCredential) {
      this.missingCredential = missingCredential;
    }
  }
}