package mosaic.region_competition.GUI;

import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.RegularizationType;

public abstract class RegularizationGUI extends GUImeMore
{
	protected RegularizationGUI(Settings settings)
	{
		super(settings);
	}
	
	public static RegularizationGUI factory(Settings settings, RegularizationType type)
	{
		RegularizationGUI result = null; 
		
		switch(type)
		{
			case Sphere_Regularization:
			{
				result = new CurvatureFlowGUI(settings);
				break;
			}
			case Sphere_Regularization_OLD: 
			{
				result = new CurvatureFlowGUI(settings);
				break;
			}
			default:
			{
				result = new DefaultRegularizationGUI();
				break;
			}
		}
		return result;
	}
	
	public static RegularizationGUI factory(Settings settings, String regularization)
	{
		RegularizationType type = RegularizationType.valueOf(regularization);
		return factory(settings, type);
	}
	

}
	
	
class CurvatureFlowGUI extends RegularizationGUI
{
	public CurvatureFlowGUI(Settings settings)
	{
		super(settings);
	}

	@Override
	public void createDialog()
	{
		gd.setTitle("Curvature Based Gradient Flow Options");
		gd.addNumericField("Local_Radius", settings.m_CurvatureMaskRadius, 0);
	}

	@Override
	public void process()
	{
		settings.m_CurvatureMaskRadius = (int)gd.getNextNumber();
	}
}

class DefaultRegularizationGUI extends RegularizationGUI
{
	protected DefaultRegularizationGUI()
	{
		super(null);
	}

	@Override
	public void createDialog()
	{
		gd = getNoGUI();
	}

	@Override
	public void process(){}
	
}




