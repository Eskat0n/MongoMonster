import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseButton
import org.bson.Document
import org.litote.kmongo.KMongo
import tornadofx.*
import com.mongodb.BasicDBObject



class MongoMonsterApp : App(MongoMonsterView::class) {
    init {
//        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
//        StyleManager.getInstance().addUserAgentStylesheet(DockPane::class.java.getResource("default.css").toExternalForm())
    }
}

class ServerModel : ViewModel() {
    private val client = KMongo.createClient()

    val alias = SimpleStringProperty("localhost")
    val databases: ObservableList<DatabaseModel>

    init {
        databases = client.listDatabaseNames()
            .map { DatabaseModel(client.getDatabase(it), client) }
            .toMutableList()
            .observable()
    }
}

class DatabaseModel(
    val database: MongoDatabase,
    val client: MongoClient
) : ViewModel() {
    val name = SimpleStringProperty(database.name)
    val collections: ObservableList<CollectionModel>
    val functions = listOf<FunctionModel>().observable()
    val users = listOf<UserModel>().observable()
    val isSpecial = booleanBinding(name) { false }

    init {
        collections = database.listCollectionNames()
            .map { CollectionModel(database, database.getCollection(it), client) }
            .toMutableList()
            .observable()
    }
}

class FunctionModel :ViewModel()
class UserModel :ViewModel()

class CollectionModel(
    val database: MongoDatabase,
    val collection: MongoCollection<Document>,
    val client: MongoClient
) : ViewModel() {
    val name = SimpleStringProperty(collection.namespace.collectionName)
    val indexes = listOf<IndexModel>().observable()

    private val queryBody = "db.getCollection('${name.value}').find({})"

    fun createQuery() = QueryModel(queryBody, database, collection)
}

class IndexModel: ViewModel()

class MongoMonsterView : View("MongoMonster") {
    private val menuView: MenuView by inject()
    private val workspaceView: WorkspaceView by inject()

    override val root = vbox {
        add(menuView)
        add(workspaceView)
    }
}

class NewQueryEvent(val query: QueryModel) : FXEvent()

class WorkspaceModel : ViewModel() {
    val queries = mutableListOf<QueryModel>().observable()

    init {
        queries.addListener { change: ListChangeListener.Change<out QueryModel> ->
            while (change.next()) {
                when {
                    change.wasAdded() -> change.addedSubList.forEach { fire(NewQueryEvent(it)) }
                }
            }
        }
    }
}

class QueryModel(
    body: String,
    private val database: MongoDatabase,
    private val collection: MongoCollection<Document>
) : ViewModel() {
    val body = SimpleStringProperty(body)

//    fun execute() = database.runCommand(BasicDBObject().apply { put("eval", body.value) })
    fun execute() = collection.find()
}

class ServerExplorerView : View("Server Explorer") {
    private val workspaceModel: WorkspaceModel by inject()
    private val serverModel: ServerModel by inject()

    override val root = treeview<Any> {
        root = TreeItem(serverModel.alias.value)
        cellFormat {
            text = when (it) {
                is String -> it
                is DatabaseModel -> "${it.name.value} (${it.collections.count()})"
                is CollectionModel -> it.name.value
                else -> {
                    kotlin.error("Invalid value type")
                }
            }
            setOnMouseClicked {mouseEvent ->
                if (mouseEvent.button ==  MouseButton.PRIMARY && mouseEvent.clickCount == 2) {
                    executeDefaultActionFor(it)
                }
            }
        }
        populate { parent ->
            val value = parent.value
            when (value) {
                is String -> serverModel.databases
                is DatabaseModel -> value.collections
                is CollectionModel -> null
                else -> null
            }
        }
    }

    private fun executeDefaultActionFor(treeItem: Any) {
        when (treeItem) {
            is CollectionModel -> workspaceModel.queries.add(treeItem.createQuery())
        }
    }

    private enum class SpecialFolder {

    }
}

class WorkspaceView : View("Workspace") {
    private val serverExplorer: ServerExplorerView by inject()
    private val queryTabManager: QueryTabManagerView by inject()

    private val workspaceModel: WorkspaceModel by inject()

    override val root = hbox {
        add(serverExplorer)
        add(queryTabManager)
    }
}

class QueryTabManagerView : View("Query Tab Manager") {
    override val root =  tabpane {
        subscribe<NewQueryEvent> {
            val tabScope = Scope()
            setInScope(it.query, tabScope)
            tab(find(QueryTabView::class, tabScope))
        }
    }
}

class QueryTabView : View("Query") {
    private val queryModel: QueryModel by inject()

    override val root = vbox {
        hbox {

        }
        textarea(queryModel.body) {  }
    }

    init {
        titleProperty.bind(queryModel.body)
        val execute = queryModel.execute()
    }
}

class MenuView : View("Menu") {
    override val root = menubar {

    }
}