/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.descartes.teastore.webui.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import tools.descartes.research.faasteastorelibrary.interfaces.persistence.CategoryEntity;
import tools.descartes.research.faasteastorelibrary.interfaces.persistence.ProductEntity;
import tools.descartes.research.faasteastorelibrary.requests.ResponseObject;
import tools.descartes.research.faasteastorelibrary.requests.category.GetAllCategoriesRequest;
import tools.descartes.research.faasteastorelibrary.requests.category.GetCategoryByIdRequest;
import tools.descartes.research.faasteastorelibrary.requests.product.GetAllProductsOfCategoryByIdRequest;
import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.loadbalancers.LoadBalancerTimeoutException;
import tools.descartes.teastore.registryclient.loadbalancers.ServiceLoadBalancer;
import tools.descartes.teastore.registryclient.rest.HttpWrapper;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedImageOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedStoreOperations;
import tools.descartes.teastore.registryclient.rest.ResponseWrapper;
import tools.descartes.teastore.entities.Category;
import tools.descartes.teastore.entities.ImageSizePreset;
import tools.descartes.teastore.entities.Product;
import tools.descartes.teastore.webui.authentication.AuthenticatorSingleton;

/**
 * Servlet implementation for the web view of "Category".
 *
 * @author Andre Bauer
 */
@WebServlet( "/category" )
public class CategoryServlet extends AbstractUIServlet
{
    private static final long serialVersionUID = 1L;

    private static final int INITIAL_PRODUCT_DISPLAY_COUNT = 20;
    private static final List< Integer > PRODUCT_DISPLAY_COUNT_OPTIONS = Arrays.asList( 5, 10, 20, 30, 50 );

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger( CategoryServlet.class );


    /**
     * @see HttpServlet#HttpServlet()
     */
    public CategoryServlet( )
    {
        super( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleGETRequest( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException, LoadBalancerTimeoutException
    {
        LOG.info( "method" );

        if ( request.getParameter( "category" ) != null )
        {
            LOG.info( "jumped into method" );

            //TODO
            checkforCookie( request, response );

            long categoryID = Long.parseLong( request.getParameter( "category" ) );

            ResponseObject< CategoryEntity > responseObject = getCategoryById( categoryID );
            CategoryEntity category = responseObject.getEntity( );
            int totalProducts = Integer.parseInt( responseObject.getHeader( "X-Total-Number-Of-Results" ) );

            LOG.info( "totalProducts: " + totalProducts );

            int numberOfProductsPerPage = INITIAL_PRODUCT_DISPLAY_COUNT;
            if ( request.getAttribute( "numberProducts" ) != null )
            {
                numberOfProductsPerPage = Integer.parseInt( request.getAttribute( "numberProducts" ).toString( ) );
            }

            int page = 1;
            if ( request.getParameter( "page" ) != null )
            {
                int pagenumber = Integer.parseInt( request.getParameter( "page" ) );
                int maxpages = ( int ) Math.ceil( ( ( double ) totalProducts ) / numberOfProductsPerPage );
                if ( pagenumber <= maxpages )
                {
                    page = pagenumber;
                }
            }

            ArrayList< String > navigation = createNavigation( totalProducts, page, numberOfProductsPerPage );

            int startIndex = ( page - 1 ) * numberOfProductsPerPage;
            int limit = numberOfProductsPerPage;

            List< ProductEntity > productList = getAllProductsOfCategoryById( startIndex, limit, categoryID );

            //TODO (in product_item.jsp)
//            request.setAttribute( "productImages", LoadBalancedImageOperations.getProductPreviewImages( productList
// ) );
//            request.setAttribute( "storeIcon",
//                    LoadBalancedImageOperations.getWebImage( "icon", ImageSizePreset.ICON.getSize( ) ) );
            request.setAttribute( "CategoryList", getAllCategories( ) );
            request.setAttribute( "title", "TeaStore Category " + category.getName( ) );
            request.setAttribute( "Productslist", productList );
            request.setAttribute( "category", category.getName( ) );
            request.setAttribute( "login", isLoggedIn( ) );
            request.setAttribute( "categoryID", categoryID );
            request.setAttribute( "currentnumber", numberOfProductsPerPage );
            request.setAttribute( "pagination", navigation );
            request.setAttribute( "pagenumber", page );
            request.setAttribute( "productdisplaycountoptions", PRODUCT_DISPLAY_COUNT_OPTIONS );

            request.getRequestDispatcher( "WEB-INF/pages/category.jsp" ).forward( request, response );
        }
        else
        {
            redirect( "/", response );
        }
    }

    private ResponseObject< CategoryEntity > getCategoryById( final long categoryId )
    {
        return new GetCategoryByIdRequest( categoryId ).performRequest( );
    }

    private List< ProductEntity > getAllProductsOfCategoryById( final int startIndex, final int limit,
            final long categoryId )
    {
        return new GetAllProductsOfCategoryByIdRequest( startIndex, limit, categoryId ).performRequest( ).getEntity( );
    }

    private boolean isLoggedIn( )
    {
        return AuthenticatorSingleton.getInstance( ).isUserLoggedIn( );
    }

    private List< CategoryEntity > getAllCategories( )
    {
        return new GetAllCategoriesRequest( 0, 10 ).performRequest( ).getEntity( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handlePOSTRequest( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException, LoadBalancerTimeoutException
    {
        if ( request.getParameter( "number" ) != null && request.getParameter( "page" ) != null
                && request.getParameter( "category" ) != null )
        {
            redirect(
                    "/category?category=" + request.getParameter( "category" ) + "&page="
                            + request.getParameter( "page" ),
                    response, PRODUCTCOOKIE, request.getParameter( "number" ) );
        }
        else
        {
            handleGETRequest( request, response );
        }
    }

    /**
     * Creates the entries for the pagination.
     *
     * @param products
     * @param page
     * @param numberProducts
     * @return Arraylist<String> pagination
     */
    private ArrayList< String > createNavigation( int products, int page, int numberProducts )
    {
        ArrayList< String > navigation = new ArrayList< String >( );

        int numberpagination = 5;

        int maxpages = ( int ) Math.ceil( ( ( double ) products ) / numberProducts );

        if ( maxpages < page )
        {
            return navigation;
        }

        if ( page == 1 )
        {
            if ( maxpages == 1 )
            {
                navigation.add( "1" );
                return navigation;
            }
            int min = Math.min( maxpages, numberpagination + 1 );
            for ( int i = 1; i <= min; i++ )
            {
                navigation.add( String.valueOf( i ) );
            }

        }
        else
        {
            navigation.add( "previous" );
            if ( page == maxpages )
            {
                int max = Math.max( maxpages - numberpagination, 1 );
                for ( int i = max; i <= maxpages; i++ )
                {
                    navigation.add( String.valueOf( i ) );
                }
                return navigation;

            }
            else
            {
                int lowerbound = ( int ) Math.ceil( ( ( double ) numberpagination - 1.0 ) / 2.0 );
                int upperbound = ( int ) Math.floor( ( ( double ) numberpagination - 1.0 ) / 2.0 );
                int up = Math.min( page + upperbound, maxpages );
                int down = Math.max( page - lowerbound, 1 );
                for ( int i = down; i <= up; i++ )
                {
                    navigation.add( String.valueOf( i ) );
                }
            }
        }
        navigation.add( "next" );

        return navigation;
    }

}
