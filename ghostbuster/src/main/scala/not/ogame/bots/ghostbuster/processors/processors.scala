package not.ogame.bots.ghostbuster

import scala.reflect.ClassTag

package object processors {
  private[processors] def checkAlreadyInQueue[T: ClassTag](tasks: List[Task]): Boolean = {
    tasks.exists(t => implicitly[ClassTag[T]].runtimeClass.isInstance(t))
  }

  private[processors] def isBuildingInQueue(tasks: List[Task]): Boolean = {
    checkAlreadyInQueue[Task.BuildSupply](tasks) || checkAlreadyInQueue[Task.BuildFacility](tasks)
  }
}
