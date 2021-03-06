package com.duboue.meetdle

import javax.servlet.http._

class MeetdleServlet extends HttpServlet {
	
	val engine = new Engine(DataStorageLogger.getInstance())
	
	@throws(classOf[java.io.IOException])
    override def doGet(req: HttpServletRequest , resp: HttpServletResponse) = {
        resp.setContentType("text/plain");
        val action=req.getParameter("action");
        if(action==null)
        	resp.getWriter().println("Hello, world");
        else if(action.equals("dump")){
        	for(p <- engine.getLogger.allPolls)
        	  for(tr <- engine.getLogger.replay(p))
        		  resp.getWriter().println(tr.toString)
        } else if(action.equals("poll")){
        	val poll = engine(req.getParameter("id").toInt)
       		resp.getWriter().println(poll.title+"\n\n"+poll.description)
        } else if(action.equals("do")){
            val tr=Transaction(req.getParameter("tr").split("\\s+").toList)
            try{
        	engine.execute(tr)
            }catch{
              case e : Exception => req.getParameter(e.getMessage())
            }
       		resp.getWriter().println("Done.")
        }
    }

}