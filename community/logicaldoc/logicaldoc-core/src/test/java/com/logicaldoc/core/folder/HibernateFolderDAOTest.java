package com.logicaldoc.core.folder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.logicaldoc.core.AbstractCoreTCase;
import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.PersistentObject;
import com.logicaldoc.core.document.AbstractDocument;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.DocumentHistory;
import com.logicaldoc.core.document.DocumentManager;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.metadata.Template;
import com.logicaldoc.core.metadata.TemplateDAO;
import com.logicaldoc.core.security.Permission;
import com.logicaldoc.core.security.Tenant;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.dao.UserDAO;

import junit.framework.Assert;

/**
 * Test case for <code>HibernateFolderDAOTest</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.0
 */
public class HibernateFolderDAOTest extends AbstractCoreTCase {

	// Instance under test
	private FolderDAO dao;

	private UserDAO userDao;

	private DocumentDAO docDao;

	private DocumentManager docManager;

	private FolderHistoryDAO historyDao;

	private TemplateDAO templateDao;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		// Retrieve the instance under test from spring context. Make sure that
		// it is an HibernateFolderDAO
		dao = (FolderDAO) context.getBean("FolderDAO");
		userDao = (UserDAO) context.getBean("UserDAO");
		docDao = (DocumentDAO) context.getBean("DocumentDAO");
		historyDao = (FolderHistoryDAO) context.getBean("FolderHistoryDAO");
		templateDao = (TemplateDAO) context.getBean("TemplateDAO");
		docManager = (DocumentManager) context.getBean("DocumentManager");
	}

	@Test
	public void testCreatePath() throws Exception {
		Folder docsFolder = dao.findById(6);
		Folder folder = dao.createPath(docsFolder, "/pippo/pluto/paperino", true, null);
		Assert.assertEquals("paperino", folder.getName());
		folder = dao.findById(folder.getParentId());
		Assert.assertEquals("pluto", folder.getName());
		folder = dao.findById(folder.getParentId());
		Assert.assertEquals("pippo", folder.getName());

		folder = dao.createPath(docsFolder, "/pippo/pluto/paperino", true, null);
		Assert.assertEquals("paperino", folder.getName());
		folder = dao.findById(folder.getParentId());
		Assert.assertEquals("pluto", folder.getName());
		folder = dao.findById(folder.getParentId());
		Assert.assertEquals("pippo", folder.getName());

		folder = dao.createPath(dao.findDefaultWorkspace(Tenant.DEFAULT_ID), "/pippo/research & development", true,
				null);
		Assert.assertNotNull(folder);
		Assert.assertEquals("research & development", folder.getName());
		Folder pippo = dao.findByName("pippo", 1L).get(0);
		Assert.assertNotNull(pippo);
		Folder research = dao.findByName("research & development", 1L).get(0);
		Assert.assertNotNull(research);

		Folder f = dao.findByPathExtended("/Default/pippo/research & development", 1L);
		Assert.assertNotNull(f);
		Assert.assertEquals(folder.getId(), f.getId());

		folder = dao.createPath(dao.findDefaultWorkspace(Tenant.DEFAULT_ID), "/pippo/research  development", true,
				null);
		Assert.assertNotNull(folder);
		Assert.assertEquals("research  development", folder.getName());
	}

	@Test
	public void testFind() {
		Folder folder = dao.findByPathExtended("/test", 1L);
		Assert.assertNotNull(folder);
		Assert.assertEquals("test", folder.getName());
		Assert.assertEquals(1200, folder.getId());

		folder = dao.findByPathExtended("/test/ABC/xyz", 1L);
		Assert.assertNotNull(folder);
		Assert.assertEquals("xyz", folder.getName());
		Assert.assertEquals(1202, folder.getId());

		folder = dao.findByPathExtended("/test/ABC/qqq", 1L);
		Assert.assertNull(folder);
	}

	@Test
	public void testCountDocuments() {
		long count = dao.countDocsInTree(5L);
		Assert.assertEquals(3, count);

		count = dao.countDocsInTree(4L);
		Assert.assertEquals(0, count);
	}

	@Test
	public void testComputeTreeSize() {
		long size = dao.computeTreeSize(5L);
		Assert.assertEquals(268704L, size);

		size = dao.computeTreeSize(4L);
		Assert.assertEquals(0, size);
	}

	@Test
	public void testDeleteTree() throws Exception {
		Assert.assertNotNull(dao.findById(1200));
		Assert.assertNotNull(dao.findById(1202));
		User user = new User();
		user.setUsername("admin");
		user.setId(1);
		FolderHistory history = new FolderHistory();
		history.setUser(user);
		dao.deleteTree(1200L, PersistentObject.DELETED_CODE_DEFAULT, history);
		Assert.assertNull(dao.findById(1200));
	}

	@Test
	public void testCopy() throws Exception {
		/*
		 * Create a tree and populate it
		 */
		Folder defaultWorkspace = dao.findById(Folder.DEFAULTWORKSPACEID);
		Folder newFolder = dao.createPath(defaultWorkspace, "pippo/pluto", true, null);
		Assert.assertNotNull(newFolder);

		Folder source = dao.findByPathExtended("/Default/pippo", Tenant.DEFAULT_ID);
		Assert.assertNotNull(source);

		User user = userDao.findByUsername("admin");

		DocumentHistory transaction = new DocumentHistory();
		transaction.setFolderId(103L);
		transaction.setUser(user);
		transaction.setUserId(1L);
		transaction.setNotified(0);
		transaction.setComment("pippo_reason");

		Document doc = docDao.findById(1);
		docDao.initialize(doc);
		doc.setCustomId(null);
		doc.setFolder(source);
		Assert.assertNotNull(docManager.create(new FileInputStream("pom.xml"), doc, transaction));

		doc = docDao.findById(1);
		docDao.initialize(doc);
		doc.setCustomId(null);
		doc.setFolder(dao.findByPathExtended("/Default/pippo/pluto", Tenant.DEFAULT_ID));
		Assert.assertNotNull(docManager.create(new FileInputStream("pom.xml"), doc, transaction));

		FolderHistory tr = new FolderHistory();
		tr.setNotified(0);
		tr.setComment("");
		tr.setUser(user);

		/*
		 * Now create a target folder and copy there inside
		 */
		Folder target = dao.createPath(defaultWorkspace, "target", true, null);
		dao.initialize(target);
		Assert.assertNotNull(target);
		target.setTemplate(templateDao.findByName("email", Tenant.DEFAULT_ID));
		target.setValue("from", "test@acme.com");
		dao.store(target);
		target = dao.findById(target.getId());
		dao.initialize(target);
		Assert.assertEquals("email", target.getTemplate().getName());
		Assert.assertEquals("test@acme.com", target.getValue("from"));

		dao.copy(source, target, null, false, "inherit", tr);

		Folder folder = dao.findByPathExtended("/Default/target/pippo/pluto", Tenant.DEFAULT_ID);
		dao.initialize(target);
		Assert.assertNotNull(folder);
		dao.initialize(folder);
		Assert.assertEquals("email", folder.getTemplate().getName());
		Assert.assertEquals("test@acme.com", folder.getValue("from"));

		List<Document> docs = docDao.findByFolder(folder.getId(), null);
		Assert.assertEquals(1, docs.size());

		folder = dao.findById(6L);
		dao.initialize(folder);
		Assert.assertEquals(3, folder.getFolderGroups().size());

		newFolder = dao.copy(folder, dao.findById(1210L), null, false, "replicate", tr);
		dao.initialize(newFolder);
		Assert.assertEquals(3, newFolder.getFolderGroups().size());
	}

	@Test
	public void testCopy2() throws Exception {
		Folder defaultWorkspace = dao.findById(Folder.DEFAULTWORKSPACEID);
		dao.createPath(defaultWorkspace, "A/B/C/D", false, null);

		Folder B = dao.findByPathExtended("/Default/A/B", 1L);
		Assert.assertNull(B.getSecurityRef());
		System.out.println("B folder is " + B.getId());

		Folder C = dao.findByPathExtended("/Default/A/B/C", 1L);
		dao.initialize(C);
		Assert.assertNull(C.getSecurityRef());
		C.setSecurityRef(B.getId());
		dao.store(C);

		Folder D = dao.findByPathExtended("/Default/A/B/C/D", 1L);
		dao.initialize(D);
		Assert.assertNull(D.getSecurityRef());
		D.setSecurityRef(B.getId());
		dao.store(D);

		Folder target = dao.createPath(defaultWorkspace, "TARGET", false, null);
		Folder source = dao.findByPathExtended("/Default/A", 1L);

		FolderHistory tr = new FolderHistory();
		tr.setNotified(0);
		tr.setComment("");
		tr.setUser(userDao.findByUsername("admin"));
		dao.copy(source, target, null, false, "replicate", tr);

		B = dao.findByPathExtended("/Default/TARGET/A/B", 1L);
		C = dao.findByPathExtended("/Default/TARGET/A/B/C", 1L);
		D = dao.findByPathExtended("/Default/TARGET/A/B/C/D", 1L);
		Assert.assertEquals(B.getId(), C.getSecurityRef().longValue());
		Assert.assertEquals(B.getId(), D.getSecurityRef().longValue());
	}

	@Test
	public void testMoveFolder_Simple() throws Exception {
		Folder docsFolder = dao.findById(Folder.DEFAULTWORKSPACEID);
		Folder folderVO = new Folder();
		folderVO.setName("folderA");
		Folder folderA = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderB");
		Folder folderB = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderC");
		Folder folderC = dao.create(folderB, folderVO, true, null);

		User user = userDao.findByUsername("admin");

		FolderHistory transaction = new FolderHistory();
		transaction.setNotified(0);
		transaction.setComment("");
		transaction.setUser(user);

		dao.move(folderC, folderA, transaction);

		List<Folder> folderList = dao.findChildren(folderA.getId(), null);
		Assert.assertTrue(folderList.size() == 1);

		Assert.assertTrue(folderList.contains(folderC));
	}

	@Test
	public void testMoveFolder_Up() throws Exception {
		Folder docsFolder = dao.findById(6);
		Folder folderVO = new Folder();
		folderVO.setName("folderA");
		Folder folderA = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderB");
		Folder folderB = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderC");
		Folder folderC = dao.create(folderB, folderVO, true, null);
		folderVO.setName("folderD");
		dao.create(folderC, folderVO, true, null);
		folderVO.setName("folderE");
		dao.create(folderC, folderVO, true, null);

		User user = userDao.findByUsername("admin");

		FolderHistory transaction = new FolderHistory();
		transaction.setNotified(0);
		transaction.setComment("");
		transaction.setUser(user);

		dao.move(folderC, folderA, transaction);

		List<Folder> folderList = dao.findChildren(folderA.getId(), null);
		Assert.assertTrue(folderList.size() == 1);
		Assert.assertTrue(folderList.contains(folderC));

		folderList = dao.findChildren(folderB.getId(), null);
		Assert.assertTrue(folderList.size() == 0);
	}

	@Test
	public void testMoveFolder_UpWithDocuments() throws Exception {
		Folder docsFolder = dao.findById(6);
		Folder folderVO = new Folder();
		folderVO.setName("folderA");
		Folder folderA = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderB");
		Folder folderB = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderC");
		Folder folderC = dao.create(folderB, folderVO, true, null);
		folderVO.setName("folderD");
		Folder folderD = dao.create(folderC, folderVO, true, null);
		folderVO.setName("folderE");
		dao.create(folderC, folderVO, true, null);

		Document doc = docDao.findById(1);
		docDao.initialize(doc);
		doc.setFolder(folderC);
		doc.setIndexed(AbstractDocument.INDEX_INDEXED);
		docDao.store(doc);

		Document doc2 = docDao.findById(2);
		docDao.initialize(doc2);
		doc2.setFolder(folderD);
		doc2.setIndexed(AbstractDocument.INDEX_INDEXED);
		docDao.store(doc2);

		User user = userDao.findByUsername("admin");

		FolderHistory transaction = new FolderHistory();
		transaction.setNotified(0);
		transaction.setComment("");
		transaction.setUser(user);

		dao.move(folderC, folderA, transaction);
		Folder fc = dao.findById(folderC.getId());
		Assert.assertEquals(folderC.getName(), fc.getName());

		List<Folder> folderList = dao.findChildren(folderA.getId(), null);
		Assert.assertTrue(folderList.size() == 1);

		Assert.assertTrue(folderList.contains(folderC));

		folderList = dao.findChildren(folderB.getId(), null);
		Assert.assertTrue(folderList.size() == 0);

		List<Document> docs = docDao.findByIndexed(0);
		Assert.assertEquals(1, docs.size());

		// Check the history creation
		List<FolderHistory> folderFolderHistory = historyDao.findByFolderId(folderC.getId());
		Assert.assertTrue(folderFolderHistory.size() > 0);

		boolean eventPresent = false;
		for (FolderHistory history : folderFolderHistory) {
			if (history.getEvent().equals(FolderEvent.MOVED.toString()))
				eventPresent = true;
		}
		Assert.assertTrue(eventPresent);
	}

	@Test
	public void testMoveFolder_Down() throws Exception {
		Folder docsFolder = dao.findById(6);
		Folder folderVO = new Folder();
		folderVO.setName("folderB");
		Folder folderB = dao.create(docsFolder, folderVO, true, null);
		folderVO.setName("folderC");
		Folder folderC = dao.create(folderB, folderVO, true, null);
		folderVO.setName("folderD");
		Folder folderD = dao.create(folderC, folderVO, true, null);
		folderVO.setName("folderE");
		Folder folderE = dao.create(folderC, folderVO, true, null);
		folderVO.setName("folderF");
		dao.create(folderE, folderVO, true, null);

		User user = userDao.findByUsername("admin");

		FolderHistory transaction = new FolderHistory();
		transaction.setNotified(0);
		transaction.setComment("");
		transaction.setUser(user);

		dao.move(folderE, folderD, transaction);

		List<Folder> folderList = dao.findChildren(folderD.getId(), null);
		Assert.assertTrue(folderList.size() == 1);
		Assert.assertTrue(folderList.contains(folderE));

		folderList = dao.findChildren(folderC.getId(), null);
		Assert.assertTrue(folderList.size() == 1);
	}

	@Test
	public void testIsInPath() throws Exception {
		Assert.assertTrue(dao.isInPath(1200, 1201));
		Assert.assertTrue(dao.isInPath(1200, 1202));
		Assert.assertFalse(dao.isInPath(99, 1202));
	}

	@Test
	public void testFind2() {
		List<Folder> folders = dao.find("folder", 1L);
		Assert.assertNotNull(folders);
		Assert.assertEquals(2, folders.size());
		Folder folder = dao.findById(6);
		Folder folder2 = dao.findById(7);
		Assert.assertTrue(folders.contains(folder));
		Assert.assertTrue(folders.contains(folder2));

		folders = dao.find("folder.adminxx", 1L);
		Assert.assertEquals(0, folders.size());
		folders = dao.find("folder", 99L);
		Assert.assertEquals(0, folders.size());
	}

	@Test
	public void testStore() throws PersistenceException {
		Folder folder = new Folder();
		folder.setName("text");
		folder.setParentId(5);
		folder.setFolderGroup(new long[] { 1, 2 });
		Assert.assertTrue(dao.store(folder));

		// Test updating the security rules
		folder = dao.findById(folder.getId());
		dao.initialize(folder);
		Assert.assertTrue(folder.getFolderGroups().size() == 2);
		FolderGroup fg = new FolderGroup();
		fg.setGroupId(3L);
		fg.setPermissions(5);
		folder.addFolderGroup(fg);
		Assert.assertTrue(dao.store(folder));
		folder = dao.findById(folder.getId());
		dao.initialize(folder);
		Assert.assertTrue(folder.getFolderGroups().size() == 3);

		folder = dao.findById(6);
		dao.initialize(folder);
		Assert.assertEquals("folder6", folder.getName());
		Assert.assertEquals(3, folder.getFolderGroups().size());

		// Load an existing folder and modify it
		folder = dao.findById(6);
		dao.initialize(folder);
		Assert.assertEquals("folder6", folder.getName());

		FolderHistory transaction = new FolderHistory();
		transaction.setFolderId(folder.getId());
		transaction.setEvent(FolderEvent.RENAMED.toString());
		transaction.setUser(userDao.findById(1));
		transaction.setNotified(0);
		dao.store(folder, transaction);

		folder = dao.findById(7);
		dao.initialize(folder);
		dao.store(folder);

		folder = dao.findById(7);
		dao.initialize(folder);
		folder.setName("xxxx");
		transaction = new FolderHistory();
		transaction.setFolderId(folder.getId());
		transaction.setEvent(FolderEvent.RENAMED.toString());
		transaction.setUser(userDao.findById(1));
		transaction.setNotified(0);
		dao.store(folder, transaction);

		folder = dao.findById(6);
		dao.initialize(folder);
		folder.getFolderGroups().remove(folder.getFolderGroup(2));
		Assert.assertEquals(2, folder.getFolderGroups().size());
		Assert.assertTrue(dao.store(folder));
		folder = dao.findById(Folder.ROOTID);
		dao.initialize(folder);
		Assert.assertEquals(4, folder.getFolderGroups().size());

		folder = dao.findById(1200);
		dao.initialize(folder);
		folder.setName("pippo");
		Assert.assertTrue(dao.store(folder));
		folder = dao.findById(1202);
		Assert.assertNotNull(folder);

		folder = dao.findById(1201);
		dao.initialize(folder);
		folder.setName("pippo2");
		Assert.assertTrue(dao.store(folder));

		// Try a folder with extended attributes
		folder = dao.findById(1202);
		dao.initialize(folder);
		Assert.assertNotNull(folder);
		Assert.assertEquals("test_val_1", folder.getValue("val1"));
		folder.setValue("val1", "xyz");
		Assert.assertTrue(dao.store(folder));
		folder = dao.findById(1202);
		dao.initialize(folder);
		Assert.assertNotNull(folder);
		Assert.assertEquals("xyz", folder.getValue("val1"));
	}

	@Test
	public void testCreate() throws PersistenceException {
		Folder parent = dao.findById(1202L);

		Folder folderVO = new Folder();
		folderVO.setName("xxxx");
		Folder folder = dao.create(parent, folderVO, true, null);
		Assert.assertNotNull(folder);

		folder = dao.findById(folder.getId());
		dao.initialize(folder);
		Assert.assertEquals("test1", folder.getTemplate().getName());
		Assert.assertEquals("test_val_1", folder.getValue("val1"));
	}

	@Test
	public void testCreateAlias() throws PersistenceException {
		Folder alias = dao.createAlias(4L, 3000L, null);
		Assert.assertNotNull(alias);
		Assert.assertEquals(new Long(3000L), alias.getFoldRef());
		Assert.assertEquals(new Long(3000L), alias.getSecurityRef());
		Assert.assertEquals(new Integer(Folder.TYPE_ALIAS), new Integer(alias.getType()));
		Folder orig = dao.findById(3000L);
		Assert.assertEquals("Workspace X", orig.getName());
		Assert.assertEquals(Folder.TYPE_WORKSPACE, orig.getType());
	}

	@Test
	public void testFindAliases() throws PersistenceException {
		List<Folder> aliases = dao.findAliases(3000L, 1L);
		Assert.assertTrue(aliases.isEmpty());

		Folder alias = dao.createAlias(4L, 3000L, null);
		Assert.assertNotNull(alias);

		aliases = dao.findAliases(3000L, 1L);
		Assert.assertEquals(1, aliases.size());
		Assert.assertEquals(alias, aliases.iterator().next());
	}

	@Test
	public void testDelete() throws PersistenceException {
		Assert.assertTrue(dao.delete(1202));
		Folder folder = dao.findById(12012);
		Assert.assertNull(folder);

		DocumentDAO docDao = (DocumentDAO) context.getBean("DocumentDAO");
		docDao.delete(1202);

		// Delete a folder with documents
		Assert.assertTrue(dao.delete(1201));
		folder = dao.findById(1201);
		Assert.assertNull(folder);

		try {
			dao.delete(Folder.DEFAULTWORKSPACEID);
		} catch (Throwable t) {
			System.out.println(t.getMessage());
		}
		folder = dao.findById(Folder.DEFAULTWORKSPACEID);
		Assert.assertNotNull(Folder.DEFAULTWORKSPACEID);
	}

	@Test
	public void testFindById() {
		// Try with a folder id
		Folder folder = dao.findById(Folder.ROOTID);
		dao.initialize(folder);
		Assert.assertNotNull(folder);
		Assert.assertEquals(Folder.ROOTID, folder.getId());
		Assert.assertEquals("/", folder.getName());
		Assert.assertEquals(4, folder.getFolderGroups().size());

		// Try with unexisting id
		folder = dao.findById(99999);
		Assert.assertNull(folder);

		// Try a folder with extended attributes
		folder = dao.findById(1202);
		dao.initialize(folder);
		Assert.assertNotNull(folder);
		Assert.assertEquals("test_val_1", folder.getValue("val1"));
	}

	@Test
	public void testFindDeleted() {
		// Try with a folder id
		List<Folder> folders = dao.findDeleted(3, 100);
		Assert.assertEquals(2, folders.size());

		folders = dao.findDeleted(99, 100);
		Assert.assertEquals(0, folders.size());
	}

	@Test
	public void testFindByName() {
		// Try with existing text
		List<Folder> folders = dao.findByName("%folder%", null);
		Assert.assertNotNull(folders);
		Assert.assertEquals(2, folders.size());

		folders = (List<Folder>) dao.findByName(null, "test", null, true);
		Assert.assertNotNull(folders);
		Assert.assertEquals(1, folders.size());

		Folder parent = dao.findById(Folder.ROOTID);
		folders = (List<Folder>) dao.findByName(parent, "test", null, true);
		Assert.assertNotNull(folders);
		Assert.assertEquals(1, folders.size());

		// Try with unexisting text
		folders = dao.findByName("xxxxx", null);
		Assert.assertNotNull(folders);
		Assert.assertTrue(folders.isEmpty());

		// Try with umlauts
		Assert.assertNotNull(dao.findByName(dao.findById(Folder.DEFAULTWORKSPACEID), "�lard", null, false));
	}

	@Test
	public void testFindByUserId() {
		List<Folder> folders = dao.findByUserId(1, Folder.ROOTID);
		Assert.assertNotNull(folders);
		Assert.assertEquals(3, folders.size());

		// Try with unexisting user and folders
		folders = dao.findByUserId(1, 70);
		Assert.assertNotNull(folders);
		Assert.assertEquals(0, folders.size());

		folders = dao.findByUserId(99, Folder.ROOTID);
		Assert.assertNotNull(folders);
		Assert.assertEquals(0, folders.size());

		folders = dao.findByUserId(4, Folder.ROOTID);

		Assert.assertNotNull(folders);
		Assert.assertEquals(1, folders.size());
	}

	@Test
	public void testFindByParentId() {
		List<Folder> folders = dao.findByParentId(Folder.ROOTID);
		Assert.assertNotNull(folders);
		Assert.assertEquals(8, folders.size());

		// Try with unexisting parent
		folders = dao.findByParentId(999);
		Assert.assertNotNull(folders);
		Assert.assertEquals(0, folders.size());
	}

	@Test
	public void testIsWriteEnable() {
		Assert.assertTrue(dao.isWriteEnabled(Folder.ROOTID, 1));
		Assert.assertTrue(dao.isWriteEnabled(6, 1));
		Assert.assertTrue(dao.isWriteEnabled(1200, 3));
		Assert.assertTrue(dao.isWriteEnabled(Folder.ROOTID, 3));
		Assert.assertFalse(dao.isWriteEnabled(Folder.ROOTID, 999));
	}

	@Test
	public void testFindByPath() {
		Assert.assertNotNull(dao.findByPathExtended("/Default/Elard", 1L));
		Assert.assertNull(dao.findByPathExtended("/Default/Elard", 99L));
	}

	@Test
	public void testIsReadEnable() {
		Assert.assertTrue(dao.isReadEnabled(Folder.ROOTID, 1));
		Assert.assertTrue(dao.isReadEnabled(5, 1));
		Assert.assertFalse(dao.isReadEnabled(Folder.ROOTID, 22));
		Assert.assertFalse(dao.isReadEnabled(Folder.ROOTID, 999));
		Assert.assertTrue(dao.isReadEnabled(1200, 3));
	}

	@Test
	public void testIsPrintEnable() {
		Assert.assertTrue(dao.isPrintEnabled(Folder.ROOTID, 1L));
		Assert.assertTrue(dao.isPrintEnabled(5, 1));
		Assert.assertFalse(dao.isPrintEnabled(Folder.ROOTID, 22L));
		Assert.assertFalse(dao.isPrintEnabled(Folder.ROOTID, 999L));
		Assert.assertTrue(dao.isPrintEnabled(1200, 3));
	}

	@Test
	public void testIsPermissionEnabled() {
		Assert.assertTrue(dao.isPermissionEnabled(Permission.WRITE, Folder.ROOTID, 1));
		Assert.assertTrue(dao.isPermissionEnabled(Permission.WRITE, 6, 1));
		Assert.assertFalse(dao.isPermissionEnabled(Permission.WRITE, Folder.ROOTID, 5));
		Assert.assertFalse(dao.isPermissionEnabled(Permission.WRITE, Folder.ROOTID, 999));
		Assert.assertTrue(dao.isPermissionEnabled(Permission.WRITE, 6, 4));
	}

	@Test
	public void testGetEnabledPermissions() {
		Set<Permission> permissions = dao.getEnabledPermissions(Folder.ROOTID, 1);
		Assert.assertEquals(Permission.all().size(), permissions.size());
		Assert.assertTrue(permissions.contains(Permission.READ));
		Assert.assertTrue(permissions.contains(Permission.SECURITY));
		Assert.assertTrue(permissions.contains(Permission.SIGN));
		permissions = dao.getEnabledPermissions(6, 4);
		Assert.assertEquals(9, permissions.size());
		Assert.assertTrue(permissions.contains(Permission.READ));
		Assert.assertTrue(permissions.contains(Permission.WRITE));
		Assert.assertTrue(permissions.contains(Permission.MOVE));
		Assert.assertTrue(permissions.contains(Permission.EMAIL));
		permissions = dao.getEnabledPermissions(999, 1);
		Assert.assertEquals(Permission.all().size(), permissions.size());
	}

	@Test
	public void testFindFolderIdByUserId() {
		Collection<Long> ids = dao.findFolderIdByUserId(3, null, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(4, ids.size());

		// Try with unexisting user
		ids = dao.findFolderIdByUserId(99, null, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(0, ids.size());
	}

	@Test
	public void testFindIdByUserId() {
		Collection<Long> ids = dao.findIdByUserId(1, 1201);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
		Assert.assertTrue(ids.contains(1202L));

		ids = dao.findIdByUserId(3, 1200);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());

		ids = dao.findIdByUserId(1, 1201);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());

		// Try with unexisting user
		ids = dao.findIdByUserId(99, 1201);
		Assert.assertNotNull(ids);
		Assert.assertEquals(0, ids.size());
	}

	@Test
	public void testHasWriteAccess() {
		Folder folder = dao.findById(1200);
		Assert.assertTrue(dao.hasWriteAccess(folder, 3));
		Assert.assertFalse(dao.hasWriteAccess(folder, 5));
		folder = dao.findById(6);
		Assert.assertTrue(dao.hasWriteAccess(folder, 3));
	}

	@Test
	public void testFindByGroupId() {
		Collection<Folder> folders = dao.findByGroupId(1);
		Assert.assertEquals(9, folders.size());
		folders = dao.findByGroupId(10);
		Assert.assertEquals(0, folders.size());
		folders = dao.findByGroupId(2);
	}

	@Test
	public void testFindParents() {
		List<Folder> folders = dao.findParents(1202);
		Assert.assertEquals(3, folders.size());
		Assert.assertEquals(dao.findById(Folder.ROOTID), folders.get(0));
		Assert.assertEquals(dao.findById(1200), folders.get(1));
		Assert.assertEquals(dao.findById(1201), folders.get(2));
	}

	@Test
	public void testFindWorkspace() {
		Folder folder = dao.findWorkspace(Folder.DEFAULTWORKSPACEID);
		Assert.assertTrue(folder.isWorkspace());
		Assert.assertEquals("Default", folder.getName());

		folder = dao.findWorkspace(6L);
		Assert.assertTrue(folder.isWorkspace());
		Assert.assertEquals("Workspace X", folder.getName());
	}

	@Test
	public void testRestore() throws PersistenceException {
		Folder folder = dao.findById(1204);
		Assert.assertNull(folder);

		dao.restore(1204, 5L, null);
		folder = dao.findById(1204);
		Assert.assertNotNull(folder);
	}

	@Test
	public void testFindByNameAndParentId() {
		List<Folder> folders = dao.findByNameAndParentId("%folder%", 3000L);
		Assert.assertEquals(2, folders.size());
		Assert.assertFalse(folders.contains(dao.findById(99)));
		Assert.assertTrue(folders.contains(dao.findById(7)));
		folders = dao.findByNameAndParentId("ABC", 1200);
		Assert.assertEquals(dao.findById(1201), folders.get(0));
	}

	@Test
	public void testFindFolderIdByUserIdAndPermission() {
		Collection<Long> ids = dao.findFolderIdByUserIdAndPermission(5, Permission.WRITE, null, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(2, ids.size());

		ids = dao.findFolderIdByUserIdAndPermission(3, Permission.WRITE, null, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(3, ids.size());
	}

	@Test
	public void testFindByUserIdAndTag() throws PersistenceException {
		Folder f = dao.findById(4L);
		dao.initialize(f);
		f.addTag("xyz");
		dao.store(f);

		List<Folder> ids = dao.findByUserIdAndTag(1L, "xyz", null);
		Assert.assertNotNull(ids);
		Assert.assertEquals(1, ids.size());
	}

	@Test
	public void testComputePathExtended() {
		Assert.assertEquals("/", dao.computePathExtended(5L));
		Assert.assertEquals("/test/ABC", dao.computePathExtended(1201));
	}

	@Test
	public void testComputePath() {
		Assert.assertEquals("/", dao.computePath(5L));
		Assert.assertEquals("/1200/1201", dao.computePath(1201));
	}

	@Test
	public void testFindChildren() {
		List<Folder> dirs = dao.findChildren(1200L, 1L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(1, dirs.size());

		dirs = dao.findChildren(1201L, 3L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(1, dirs.size());
	}

	@Test
	public void testApplyRightsToTree() throws PersistenceException {
		FolderHistory transaction = new FolderHistory();
		User user = new User();
		user.setId(4);
		transaction.setUser(user);
		transaction.setNotified(0);

		// The root defines it's own policies
		Folder folder = dao.findById(1200);
		Assert.assertNull(folder.getSecurityRef());
		Assert.assertTrue(dao.applyRightToTree(1200, transaction));
		folder = dao.findById(1201);
		Assert.assertEquals(1200L, folder.getSecurityRef().longValue());
		folder = dao.findById(1202);
		Assert.assertEquals(1200L, folder.getSecurityRef().longValue());

		// The root refers to another folder's policies
		folder = dao.findById(1200);
		dao.initialize(folder);
		folder.setSecurityRef(5L);
		dao.store(folder);
		Assert.assertTrue(dao.applyRightToTree(1200, transaction));
		folder = dao.findById(1201);
		Assert.assertEquals(5L, folder.getSecurityRef().longValue());
		folder = dao.findById(1202);
		Assert.assertEquals(5L, folder.getSecurityRef().longValue());
	}

	@Test
	public void testApplyTagsToTree() throws PersistenceException {
		FolderHistory transaction = new FolderHistory();
		User user = new User();
		user.setId(4);
		transaction.setUser(user);
		transaction.setNotified(0);

		// The root defines it's own policies
		Folder folder = dao.findById(1200L);
		dao.initialize(folder);

		Assert.assertTrue(folder.getTagsAsWords().contains("ftag1"));

		folder = dao.findById(1201);
		dao.initialize(folder);
		Assert.assertTrue(folder.getTags().isEmpty());

		dao.applyTagsToTree(1200L, transaction);
		dao.findById(1201);
		dao.initialize(folder);
		Assert.assertTrue(folder.getTagsAsWords().contains("ftag1"));
	}

	@Test
	public void testUpdateSecurityRef() {
		FolderHistory transaction = new FolderHistory();
		User user = new User();
		user.setId(4);
		transaction.setUser(user);
		transaction.setNotified(0);

		// The root defines it's own policies
		Folder folder = dao.findById(1200);
		Assert.assertNull(folder.getSecurityRef());
		Assert.assertTrue(dao.applyRightToTree(1200, transaction));
		folder = dao.findById(1201);
		Assert.assertEquals(1200L, folder.getSecurityRef().longValue());
		folder = dao.findById(1202);
		Assert.assertEquals(1200L, folder.getSecurityRef().longValue());

		// The root has its own policies
		folder = dao.findById(1200);

		Assert.assertTrue(dao.updateSecurityRef(1200, 5L, transaction));

		folder = dao.findById(1200);
		Assert.assertEquals(5L, folder.getSecurityRef().longValue());
		folder = dao.findById(1201);
		Assert.assertEquals(5L, folder.getSecurityRef().longValue());
		folder = dao.findById(1202);
		Assert.assertEquals(5L, folder.getSecurityRef().longValue());
	}

	@Test
	public void testMetadataToTree() throws PersistenceException {
		FolderHistory transaction = new FolderHistory();
		User user = new User();
		user.setId(4);
		transaction.setUser(user);
		transaction.setNotified(0);

		Folder folder = dao.findById(1200);
		dao.initialize(folder);
		Assert.assertNull(folder.getTemplate());
		folder = dao.findById(1201);
		Assert.assertNull(folder.getTemplate());
		folder = dao.findById(1202);
		Assert.assertNotNull(folder.getTemplate());

		folder = dao.findById(1200);
		dao.initialize(folder);

		Template template = templateDao.findById(1L);
		folder.setTemplate(template);
		folder.setValue("attr1", "test");
		dao.store(folder);

		Assert.assertTrue(dao.applyMetadataToTree(1200, transaction));
		folder = dao.findById(1202);
		dao.initialize(folder);
		Assert.assertEquals("test1", folder.getTemplate().getName());
		Assert.assertEquals("test", folder.getValue("attr1"));

		folder = dao.findById(1201);
		dao.initialize(folder);
		Assert.assertEquals("test1", folder.getTemplate().getName());
		Assert.assertEquals("test", folder.getValue("attr1"));

		folder = dao.findById(1202);
		dao.initialize(folder);
		Assert.assertEquals("test1", folder.getTemplate().getName());
		Assert.assertEquals("test", folder.getValue("attr1"));

		folder = dao.findById(1200);
		dao.initialize(folder);
		folder.getAttributes().clear();
		folder.setTemplate(null);
		dao.store(folder);

		Assert.assertTrue(dao.applyMetadataToTree(1200, transaction));
		folder = dao.findById(1201);
		dao.initialize(folder);
		Assert.assertEquals(null, folder.getTemplate());
		folder = dao.findById(1202);
		dao.initialize(folder);
		Assert.assertEquals(null, folder.getTemplate());
	}

	@Test
	public void testFindWorkspaces() {
		List<Folder> dirs = dao.findWorkspaces(1L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(2, dirs.size());
		Assert.assertEquals("Default", dirs.get(0).getName());

		dirs = dao.findWorkspaces(99L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(0, dirs.size());
	}

	@Test
	public void testFindFolderIdInTree() {
		Collection<Long> ids = dao.findFolderIdInTree(1200L, false);
		Assert.assertEquals(3, ids.size());
		Assert.assertTrue(ids.contains(1202L));
		Assert.assertTrue(ids.contains(1201L));
		Assert.assertTrue(ids.contains(1200L));

		ids.clear();
		ids = dao.findFolderIdInTree(5L, false);
		Assert.assertEquals(9, ids.size());
		Assert.assertTrue(ids.contains(1200L));
		Assert.assertTrue(ids.contains(6L));
		Assert.assertTrue(ids.contains(3000L));

		ids.clear();
		ids = dao.findFolderIdInTree(1200L, false);
		Assert.assertEquals(3, ids.size());
		Assert.assertTrue(ids.contains(1201L));
	}

	@Test
	public void testFindbyPathExtended() {
		Folder folder = dao.findByPathExtended("/Default/Elard", 1L);
		Assert.assertNotNull(folder);
	}

	@Test
	public void testFindDefaultWorkspace() {
		Folder folder = dao.findDefaultWorkspace(1L);
		Assert.assertNotNull(folder);
		Assert.assertEquals(Folder.DEFAULTWORKSPACENAME, folder.getName());
		Assert.assertEquals(Folder.DEFAULTWORKSPACEID, folder.getId());

		folder = dao.findDefaultWorkspace(99L);
		Assert.assertNull(folder);
	}

	@Test
	public void testFindByUserAndTag() throws Exception {
		Folder folder = dao.findById(4L);
		dao.initialize(folder);
		folder.addTag("xyz");
		dao.store(folder);

		List<Folder> folders = dao.findByUserIdAndTag(1L, "xyz", null);
		Assert.assertNotNull(folders);
		Assert.assertEquals(1, folders.size());

		folders = dao.findByUserIdAndTag(1L, "unexisting", null);
		Assert.assertNotNull(folders);
		Assert.assertEquals(0, folders.size());
	}

	@Test
	public void testMerge() throws FileNotFoundException, Exception {
		Folder root = dao.findById(5L);
		dao.createPath(root, "/Default/Target/Pippo", false, null);
		dao.createPath(root, "/Default/Target/Pluto", false, null);
		dao.createPath(root, "/Default/Target/Pluto/Paperino", false, null);
		dao.createPath(root, "/Default/Target/Pluto/Paperina", false, null);

		dao.createPath(root, "/Default/Source/Pippo", false, null);
		dao.createPath(root, "/Default/Source/Pluto", false, null);
		dao.createPath(root, "/Default/Source/Pluto/Paperino", false, null);
		dao.createPath(root, "/Default/Source/Pluto/Paperina", false, null);
		dao.createPath(root, "/Default/Source/Pollo/ABC", false, null);
		dao.createPath(root, "/Default/Source/Pollo/DEF", false, null);

		User user = userDao.findByUsername("admin");
		DocumentHistory transaction = new DocumentHistory();
		transaction.setUser(user);
		transaction.setUserId(1L);
		transaction.setNotified(0);

		FolderHistory fTransaction = new FolderHistory();
		fTransaction.setUser(user);
		fTransaction.setUserId(1L);
		fTransaction.setNotified(0);

		Document doc = docDao.findById(1);
		Assert.assertNotNull(doc);

		docDao.initialize(doc);
		doc = doc.clone();
		doc.setId(0);
		doc.setFolder(dao.findByPathExtended("/Default/Target/Pippo", 1L));
		doc.setFileName("doc1.txt");
		docManager.create(new FileInputStream("pom.xml"), doc, transaction);

		doc = doc.clone();
		doc.setId(0);
		doc.setFolder(dao.findByPathExtended("/Default/Source/Pippo", 1L));
		doc.setFileName("doc1.txt");
		docManager.create(new FileInputStream("pom.xml"), doc, transaction);

		doc = doc.clone();
		doc.setId(0);
		doc.setFolder(dao.findByPathExtended("/Default/Source/Pippo", 1L));
		doc.setFileName("doc2.txt");
		docManager.create(new FileInputStream("pom.xml"), doc, transaction);

		doc = doc.clone();
		doc.setId(0);
		doc.setFolder(dao.findByPathExtended("/Default/Source/Pluto/Paperina", 1L));
		doc.setFileName("doc3.txt");
		docManager.create(new FileInputStream("pom.xml"), doc, transaction);

		doc = doc.clone();
		doc.setId(0);
		doc.setFolder(dao.findByPathExtended("/Default/Source/Pollo/DEF", 1L));
		doc.setFileName("doc4.txt");
		docManager.create(new FileInputStream("pom.xml"), doc, transaction);

		Folder target = dao.findByPathExtended("/Default/Target", 1L);
		Folder source = dao.findByPathExtended("/Default/Source", 1L);
		dao.merge(source, target, fTransaction);

		source = dao.findByPathExtended("/Default/Source", 1L);
		Assert.assertTrue(source == null || source.getDeleted() != 0);

		target = dao.findByPathExtended("/Default/Target", 1L);
		Assert.assertNotNull(target);

		Folder folder = dao.findByPathExtended("/Default/Target/Pollo/ABC", 1L);
		Assert.assertTrue(folder == null || folder.getDeleted() != 0);
		folder = dao.findByPathExtended("/Default/Target/Pollo/DEF", 1L);
		Assert.assertTrue(folder == null || folder.getDeleted() != 0);

		docDao.findByPath("/Default/Target/Pollo/DEF/doc4.txt", 1L);
	}
}