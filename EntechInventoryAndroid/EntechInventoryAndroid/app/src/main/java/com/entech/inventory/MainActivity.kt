package com.entech.inventory

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.Editable
import android.text.TextWatcher

class MainActivity : Activity() {
    private lateinit var db: InventoryDatabase
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var currentPage = "Dashboard"
    private val navy = Color.rgb(18,18,18)
    private val blue = Color.rgb(220,38,38)
    private val ink = Color.rgb(20,20,20)
    private val muted = Color.rgb(115,115,115)
    private val pale = Color.rgb(245,247,250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = InventoryDatabase(this)
        buildShell()
        showDashboard()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245,247,250))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20),dp(40),dp(20),dp(15))
            setBackgroundColor(navy)
        }
        header.addView(label("ENTECH CONTROLS",18,Color.WHITE,true))
        header.addView(label("CORPORATION  •  INVENTORY MANAGEMENT",10,Color.rgb(220,100,100),false))
        root.addView(header)
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(8,6,8,6)
        }
        listOf("Dashboard","Inventory","Reports").forEach { page ->
            val button = Button(this).apply {
                text = page
                textSize = 12f
                isAllCaps = false
                setTextColor(blue)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { currentPage=page; refreshPage() }
            }
            nav.addView(button,LinearLayout.LayoutParams(0,dp(45),1f))
        }
        root.addView(nav)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16,18,16,24)
        }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }

    private fun refreshPage() {
        when(currentPage) {
            "Dashboard" -> showDashboard()
            "Inventory" -> showInventory()
            else -> showReports()
        }
    }

    private fun showDashboard() {
        content.removeAllViews()
        val items = db.getItems()
        val total = items.sumOf { it.quantity }
        content.addView(label("Inventory Overview",23,ink,true))
        content.addView(label("Track parts, materials, components, and equipment.",13,muted,false))
        val stats = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        stats.addView(statCard("INVENTORY ITEMS",items.size.toString(),"Unique part records"))
        stats.addView(statCard("TOTAL QUANTITY",total.toString(),"Units currently on hand"))
        content.addView(stats)
        content.addView(sectionTitle("Recent Stock Activity"))
        val recent=db.getMovements().take(5)
        if(recent.isEmpty()) content.addView(note("No stock movements recorded yet."))
        else recent.forEach { content.addView(movementCard(it)) }
        content.addView(actionButton("＋  Add Inventory Item") { itemDialog(null) })
    }

    private fun showInventory() {
        content.removeAllViews()
        content.addView(label("Inventory",23,ink,true))
        content.addView(label("Add, update, search, and manage inventory.",13,muted,false))
        val search=EditText(this).apply {
            hint="Search name, part number, supplier..."
            textSize=14f
            maxLines=1
            setPadding(dp(12),dp(10),dp(12),dp(10))
            setBackgroundColor(Color.WHITE)
        }
        content.addView(search,matchWrap())
        val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        content.addView(list)
        fun populate(query:String) {
            list.removeAllViews()
            val found=db.getItems(query)
            if(found.isEmpty()) list.addView(note("No matching inventory items."))
            found.forEach { list.addView(itemCard(it,true)) }
        }
        search.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){populate(s?.toString().orEmpty())}
            override fun afterTextChanged(s:Editable?){}
        })
        populate("")
        content.addView(actionButton("＋  Add Inventory Item") { itemDialog(null) })
    }

    private fun showReports() {
        content.removeAllViews()
        val items=db.getItems()
        content.addView(label("Reports",23,ink,true))
        content.addView(label("Inventory status and material movement history.",13,muted,false))
        content.addView(sectionTitle("Current Inventory"))
        if(items.isEmpty()) content.addView(note("No inventory records."))
        items.forEach { content.addView(reportRow("${it.name}  (${it.partNumber})","Qty: ${it.quantity}  •  ${it.location}")) }
        content.addView(sectionTitle("Recently Received"))
        val received=db.getMovements("received")
        if(received.isEmpty()) content.addView(note("No receipts recorded."))
        received.forEach { content.addView(reportRow(it.itemName,"+${it.quantity}  •  ${it.date}")) }
        content.addView(sectionTitle("Inventory Usage History"))
        val used=db.getMovements("used")
        if(used.isEmpty()) content.addView(note("No usage recorded."))
        used.forEach { content.addView(reportRow(it.itemName,"−${it.quantity}  •  ${it.date}")) }
    }

    private fun itemCard(item:InventoryItem,showActions:Boolean):View {
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(14),dp(12),dp(14),dp(12))
            setBackgroundColor(Color.WHITE)
        }
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val nameBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        nameBox.addView(label(item.name,16,ink,true))
        nameBox.addView(label("${item.partNumber}  •  ${item.category}",11,muted,false))
        top.addView(nameBox,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(label("Qty ${item.quantity}",14,ink,true))
        card.addView(top)
        card.addView(label("Location: ${item.location.ifBlank{"Not specified"}}",11,muted,false))
        card.addView(label("Supplier: ${item.supplier.ifBlank{"The Shop"}}",11,muted,false))
        if(showActions) {
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.END }
            row.addView(smallButton("Receive / Use") { movementDialog(item) })
            row.addView(smallButton("Edit") { itemDialog(item) })
            row.addView(smallButton("Delete") {
                AlertDialog.Builder(this@MainActivity).setTitle("Delete item?")
                    .setMessage("Delete ${item.name} (${item.partNumber})?")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->db.deleteItem(item.id);showInventory()}
                    .show()
            })
            card.addView(row)
        }
        val outer=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,0,0,dp(9)) }
        outer.addView(card)
        return outer
    }

    private fun itemDialog(existing:InventoryItem?) {
        val fields=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(8),dp(20),0) }
        fun field(hint:String,value:String=""):EditText {
            val e=EditText(this).apply { this.hint=hint; setText(value); textSize=14f; maxLines=1 }
            fields.addView(e,matchWrap()); return e
        }
        val name=field("Part name *",existing?.name.orEmpty())
        val number=field("Part number *",existing?.partNumber.orEmpty())
        val category=Spinner(this)
        val categories=listOf("Components","Materials","Electrical","Equipment","Hardware","Other")
        category.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,categories)
        category.setSelection(categories.indexOf(existing?.category).takeIf{it>=0}?:0)
        fields.addView(category,matchWrap())
        val quantity=field("Quantity *",existing?.quantity?.toString()?: "0")
        quantity.inputType=android.text.InputType.TYPE_CLASS_NUMBER
        val location=field("Location",existing?.location.orEmpty())
        val supplierField=field("Supplier",existing?.supplier.orEmpty().ifBlank{"The Shop"})
        val description=field("Description",existing?.description.orEmpty())
        val scroll=ScrollView(this).apply { addView(fields) }
        AlertDialog.Builder(this).setTitle(if(existing==null)"Add Inventory Item" else "Edit Inventory Item")
            .setView(scroll).setNegativeButton("Cancel",null)
            .setPositiveButton("Save",null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val q=quantity.text.toString().toIntOrNull()
                        if(name.text.isBlank()||number.text.isBlank()||q==null||q<0){
                            Toast.makeText(this,"Enter a name, part number, and valid non-negative quantity.",Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val sup=supplierField.text.toString().trim().ifBlank{"The Shop"}
                        val item=InventoryItem(existing?.id?:0L,name.text.toString().trim(),number.text.toString().trim(),
                            category.selectedItem.toString(),description.text.toString(),q,0,location.text.toString(),sup)
                        if(db.saveItem(item)){dialog.dismiss();refreshPage();Toast.makeText(this,"Inventory saved.",Toast.LENGTH_SHORT).show()}
                        else Toast.makeText(this,"Could not save. Check that the part number is unique.",Toast.LENGTH_LONG).show()
                    }
                }
                dialog.show()
            }
    }

    private fun movementDialog(item:InventoryItem) {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(8),dp(20),0) }
        box.addView(label("${item.name}  •  Current quantity: ${item.quantity}",13,ink,true))
        val type=Spinner(this)
        type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Receive stock","Record usage"))
        box.addView(type,matchWrap())
        val amount=EditText(this).apply { hint="Quantity"; inputType=android.text.InputType.TYPE_CLASS_NUMBER; setText("1") }
        box.addView(amount,matchWrap())
        AlertDialog.Builder(this).setTitle("Update Stock").setView(box)
            .setNegativeButton("Cancel",null).setPositiveButton("Update",null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val qty=amount.text.toString().toIntOrNull()
                        if(qty==null||qty<=0){Toast.makeText(this,"Enter a quantity greater than zero.",Toast.LENGTH_SHORT).show();return@setOnClickListener}
                        val movementType=if(type.selectedItemPosition==0)"received" else "used"
                        if(!db.recordMovement(item,movementType,qty)){
                            Toast.makeText(this,"Not enough stock available for this usage.",Toast.LENGTH_LONG).show()
                        }else{dialog.dismiss();refreshPage();Toast.makeText(this,"Stock updated.",Toast.LENGTH_SHORT).show()}
                    }
                }
                dialog.show()
            }
    }

    private fun statCard(title:String,value:String,subtitle:String,warning:Boolean=false):View {
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(13),dp(16),dp(13)); setBackgroundColor(Color.WHITE) }
        card.addView(label(title,10,muted,true));card.addView(label(value,27,if(warning)Color.rgb(220,38,38) else ink,true))
        card.addView(label(subtitle,10,muted,false))
        val wrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(0,dp(5),0,dp(5)) };wrap.addView(card);return wrap
    }
    private fun sectionTitle(text:String):View {
        val v=label(text,17,ink,true);v.setPadding(0,dp(16),0,dp(9));return v
    }
    private fun reportRow(title:String,subtitle:String):View {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));setBackgroundColor(Color.WHITE) }
        box.addView(label(title,13,ink,true));box.addView(label(subtitle,11,muted,false))
        val outer=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(0,0,0,dp(5)) };outer.addView(box);return outer
    }
    private fun movementCard(m:StockMovement):View = reportRow(m.itemName,"${if(m.type=="received")"+" else "−"}${m.quantity}  •  ${m.date}")
    private fun note(text:String):View = label(text,12,muted,false).apply { setPadding(dp(12),dp(14),dp(12),dp(14));setBackgroundColor(Color.WHITE) }
    private fun label(text:String,size:Int,color:Int,bold:Boolean):TextView = TextView(this).apply {
        this.text=text;textSize=size.toFloat();setTextColor(color);if(bold) setTypeface(typeface,Typeface.BOLD)
        setPadding(0,dp(3),0,dp(3))
    }
    private fun actionButton(text:String,action:()->Unit):Button = Button(this).apply {
        this.text=text;isAllCaps=false;textSize=14f;setTextColor(Color.WHITE);setBackgroundColor(blue);setOnClickListener{action()}
    }
    private fun smallButton(text:String,action:()->Unit):Button = Button(this).apply {
        this.text=text;isAllCaps=false;textSize=10f;setOnClickListener{action()}
    }
    private fun matchWrap()=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(5) }
    private fun dp(value:Int):Int=(value*resources.displayMetrics.density).toInt()
}
