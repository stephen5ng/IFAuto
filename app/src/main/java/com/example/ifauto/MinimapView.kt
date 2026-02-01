package com.example.ifauto

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class MinimapView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    
    private val paintNode = Paint().apply { color = Color.parseColor("#00BCD4"); style = Paint.Style.FILL; isAntiAlias = true } // Cyan
    private val paintCurrent = Paint().apply { color = Color.parseColor("#FFC107"); style = Paint.Style.FILL; isAntiAlias = true } // Amber
    private val paintEdge = Paint().apply { color = Color.WHITE; strokeWidth = 5f; isAntiAlias = true }
    
    data class Node(val id: Int, val x: Float, val y: Float)
    private val nodes = mutableMapOf<Int, Node>()
    private val edges = mutableListOf<Pair<Int, Int>>()
    
    private var currentId = -1
    
    // Grid unit size
    private val UNIT = 60f
    
    fun updateMap(newId: Int, lastMove: String?) {
        if (newId == -1) return
        
        if (nodes.isEmpty()) {
            nodes[newId] = Node(newId, 0f, 0f)
            currentId = newId
            invalidate()
            return
        }
        
        // Only update if we moved to a DIFFERENT room
        if (newId != currentId && lastMove != null) {
            val prev = nodes[currentId]
            
            // If we have a previous node record, calculate the new one relative to it
            if (prev != null && !nodes.containsKey(newId)) {
                var nx = prev.x
                var ny = prev.y
                
                when (lastMove.lowercase().trim()) {
                    "north", "n" -> ny -= 1
                    "south", "s" -> ny += 1
                    "east", "e" -> nx += 1
                    "west", "w" -> nx -= 1
                    "ne" -> { nx+=1; ny-=1 }
                    "nw" -> { nx-=1; ny-=1 }
                    "se" -> { nx+=1; ny+=1 }
                    "sw" -> { nx-=1; ny+=1 }
                    "up", "u" -> { ny-=0.5f; nx+=0.5f } // Isometrics?
                    "down", "d" -> { ny+=0.5f; nx-=0.5f }
                }
                nodes[newId] = Node(newId, nx, ny)
            }
            
            // Add connection if not exists
            val pair = currentId to newId
            val rev = newId to currentId
            if (!edges.contains(pair) && !edges.contains(rev)) {
                edges.add(pair)
            }
            
            currentId = newId
            invalidate()
        } else if (newId != currentId) {
             // Moved but no direction? Teleport?
             // Just set current
             currentId = newId
             invalidate()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val curr = nodes[currentId]
        
        // If map is empty or just started, draw nothing or one dot
        if (curr == null) return
        
        val cx = width / 2f
        val cy = height / 2f
        
        canvas.save()
        // Translate so current node is in center
        canvas.translate(cx - curr.x * UNIT, cy - curr.y * UNIT)
        
        // Draw Edges
        for ((u, v) in edges) {
            val n1 = nodes[u] ?: continue
            val n2 = nodes[v] ?: continue
            canvas.drawLine(n1.x * UNIT, n1.y * UNIT, n2.x * UNIT, n2.y * UNIT, paintEdge)
        }
        
        // Draw Nodes
        for (node in nodes.values) {
            val p = if (node.id == currentId) paintCurrent else paintNode
            val r = if (node.id == currentId) 20f else 15f
            canvas.drawCircle(node.x * UNIT, node.y * UNIT, r, p)
        }
        
        canvas.restore()
    }
}
