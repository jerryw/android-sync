/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidTypeStoreDelegate;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.BookmarkNeedsReparentingException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class AndroidBrowserBookmarksRepositoryTest extends AndroidBrowserRepositoryTest {
  
  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserBookmarksRepository();
  }
  
  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserBookmarksDataAccessor(getApplicationContext());
  }
 
  // NOTE NOTE NOTE
  // Currently queries require the qualifier (i.e. "bookmarks.guid"), inserts
  // require that the qualifier is not there. Oh...and the qualifier is only
  // required for columns that exist in multiple tables (like guid, url, etc.)
  // UGLY, MESSY, and with some luck and cooperation from #mobile TEMPORARY!
  
  // ALSO must store folder before records if we we are checking that the
  // records returned are the same as those sent in. If you don't want to
  // store a folder first, store your record in "mobile" or one of the fodlers
  // that always exists.
  
  @Override
  public void testFetchAll() {
    Record[] expected = new Record[3];
    expected[0] = BookmarkHelpers.createFolder1();
    expected[1] = BookmarkHelpers.createBookmark1();
    expected[2] = BookmarkHelpers.createBookmark2();
    basicFetchAllTest(expected);
  }
  
  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  @Override
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(BookmarkHelpers.createBookmarkInMobileFolder1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(BookmarkHelpers.createBookmarkInMobileFolder1(),
        BookmarkHelpers.createBookmarkInMobileFolder2());
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(BookmarkHelpers.createBookmark1());
  }

  @Override
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(BookmarkHelpers.createBookmarkInMobileFolder1(),
        BookmarkHelpers.createBookmarkInMobileFolder2());
  }
  
  @Override
  public void testFetchMultipleRecordsByGuids() {
    BookmarkRecord record0 = BookmarkHelpers.createFolder1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record2 = BookmarkHelpers.createBookmark2();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  @Override
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(BookmarkHelpers.createBookmark1());
  }
  
    
  @Override
  public void testWipe() {
    doWipe(BookmarkHelpers.createBookmarkInMobileFolder1(), BookmarkHelpers.createBookmarkInMobileFolder2());
  }
  
  @Override
  public void testStore() {
    basicStoreTest(BookmarkHelpers.createBookmark1());
  }

  /*
   * Test storing each different type of Bookmark record.
   * We expect any records with type other than "bookmark"
   * or "folder" to fail. For now we throw these away.
   */
  public void testStoreMicrosummary() {
    basicStoreFailTest(BookmarkHelpers.createMicrosummary());
  }

  public void testStoreQuery() {
    basicStoreFailTest(BookmarkHelpers.createQuery());
  }

  public void testStoreFolder() {
    basicStoreTest(BookmarkHelpers.createFolder1());
  }

  public void testStoreLivemark() {
    basicStoreFailTest(BookmarkHelpers.createLivemark());
  }

  public void testStoreSeparator() {
    basicStoreFailTest(BookmarkHelpers.createSeparator());
  }
  
  protected void basicStoreFailTest(Record record) {
    prepSession();    
    performWait(storeRunnable(getSession(), record, new ExpectInvalidTypeStoreDelegate()));
  }
  
  /*
   * Re-parenting tests
   */
  // Insert two records missing parent, then insert their parent.
  // Make sure they end up with the correct parent on fetch.
  public void testBasicReparenting() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createFolder1()
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  // Insert 3 folders and 4 bookmarks in different orders
  // and make sure they come out parented correctly
  public void testMultipleFolderReparenting1() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createBookmark3(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark4(),
        BookmarkHelpers.createFolder3(),
        BookmarkHelpers.createFolder2(),
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  public void testMultipleFolderReparenting2() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createBookmark3(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark4(),
        BookmarkHelpers.createFolder3(),
        BookmarkHelpers.createFolder2(),
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  public void testMultipleFolderReparenting3() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createBookmark3(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark4(),
        BookmarkHelpers.createFolder3(),
        BookmarkHelpers.createFolder2(),
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  private void doMultipleFolderReparentingTest(Record[] expected) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    doStore(session, expected);
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(expected);
    performWait(fetchAllRunnable(session, delegate));
    session.finish(new ExpectFinishDelegate());
  }
  
  
  // Insert a record without a parent and check that it is
  // put into unfiled bookmarks. Call finish() and check
  // for an error returned stating that there are still
  // records that need to be re-parented.
  public void testFinishBeforeReparent() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    Record[] records = new Record[] {
      BookmarkHelpers.createBookmark1()  
    };
    doStore(session, records);
    session.finish(new DefaultFinishDelegate() {
      @Override
      public void onFinishFailed(Exception ex) {
        if (ex.getClass() != BookmarkNeedsReparentingException.class) {
          fail("Expected: " + BookmarkNeedsReparentingException.class + " but got " + ex.getClass());
        }
      }
    });
  }
  
  /*
   * Test storing identical records with different guids.
   * For bookmarks identical is defined by the following fields
   * being the same: title, uri, type, parentName
   */
  @Override
  public void testStoreIdenticalExceptGuid() {
    Record record0 = BookmarkHelpers.createBookmarkInMobileFolder1();
    Record record1 = BookmarkHelpers.createBookmarkInMobileFolder1();
    record1.guid = Utils.generateGuid();
    assert(!record0.guid.equals(record1.guid));
    storeIdenticalExceptGuid(record0, record1);
  }
  
  /*
   * More complicated situation in which we insert a folder
   * followed by a couple of its children. We then insert
   * the folder again but with a different guid. Children
   * must still get correct parent when they are fetched.
   * Store a record after with the new guid as the parent
   * and make sure it works as well.
   */
  public void testStoreIdenticalFoldersWithChildren() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    Record record0 = BookmarkHelpers.createFolder1();
    performWait(storeRunnable(session, record0));
    
    // Get timestamp so that the conflicting folder that we store below is newer
    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { record0 });
    performWait(fetchRunnable(session, new String[] { record0.guid }, timestampDelegate));
    
    Record record1 = BookmarkHelpers.createBookmark1();
    Record record2 = BookmarkHelpers.createBookmark2();
    Record record3 = BookmarkHelpers.createFolder1();
    BookmarkRecord bmk3 = (BookmarkRecord) record3;
    record3.guid = Utils.generateGuid();
    record3.lastModified = timestampDelegate.records.get(0).lastModified + 3000;
    assert(!record0.guid.equals(record3.guid));
    
    // Store an additional record after duplicate folder inserted
    // with new guid and make sure it comes back as well 
    Record record4 = BookmarkHelpers.createBookmark3();
    BookmarkRecord bmk4 = (BookmarkRecord) record4;
    bmk4.parentID = bmk3.guid;
    bmk4.parentName = bmk3.parentName;
    
    doStore(session, new Record[] {
      record1, record2, record3, bmk4
    });
    BookmarkRecord bmk1 = (BookmarkRecord) record1;
    bmk1.parentID = record3.guid;
    BookmarkRecord bmk2 = (BookmarkRecord) record2;
    bmk2.parentID = record3.guid;
    Record[] expect = new Record[] {
        bmk1, bmk2, record3
    };
    fetchAllRunnable(session, new ExpectFetchDelegate(expect));
  }
  
  @Override
  public void testRemoteNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    localNewerTimeStamp(local, remote);
  }
  
  @Override
  public void testDeleteRemoteNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    deleteRemoteNewer(local, remote);
  }
  
  @Override
  public void testDeleteLocalNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    deleteLocalNewer(local, remote);
  }
  
  @Override
  public void testDeleteRemoteLocalNonexistent() {
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    deleteRemoteLocalNonexistent(remote);
  }

  @Override
  public void testCleanMultipleRecords() {
    // TODO Auto-generated method stub
    Log.w(tag, "Not implemented yet, waiting on deleted field");
  }

  @Override
  public void testCleanSuccessFalse() {
    // TODO Auto-generated method stub
    Log.w(tag, "Not implemented yet, waiting on deleted field");
  }

  /*
   * TODO put these back in once we have a "deleted" column.
   * Also check into deletion tests once that column is in.
  @Override
  public void testCleanMultipleRecords() {
    cleanMultipleRecords(
        BookmarkHelpers.createBookmarkInMobileFolder1(),
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createFolder1());
  }

  @Override
  public void testCleanSuccessFalse() {
    cleanSuccessFalse(BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createFolder1());
  }
  */
  
  public void testBasicPositioning() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark2()
    };
    doStore(session, expected);
    
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(expected);
    performWait(fetchAllRunnable(session, delegate));
    
    int found = 0;
    for (int i = 0; i < delegate.records.size(); i++) {
      BookmarkRecord rec = (BookmarkRecord) delegate.records.get(i);
      if (rec.guid.equals(expected[0].guid)) {
        assertEquals(0, ((BookmarkRecord) delegate.records.get(i)).androidPosition);
        found++;
      } else if (rec.guid.equals(expected[2].guid)) {
        assertEquals(1, ((BookmarkRecord) delegate.records.get(i)).androidPosition);
        found++;
      }
    }
    assertEquals(2, found);
    
  }
  
}
